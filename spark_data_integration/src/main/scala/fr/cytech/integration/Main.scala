package fr.cytech.integration

import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.spark.sql.SparkSession
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.{CreateBucketRequest, HeadBucketRequest, NoSuchBucketException}

import java.io.File
import java.net.{HttpURLConnection, URI, URL}
import java.nio.file.{Files, StandardCopyOption}
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.{Map => JMap}
import scala.io.Source
import scala.util.{Failure, Success, Try, Using}

/** Exercice 1 : collecte des données NYC Yellow Taxi.
  *
  * Pour chacun des N derniers mois disponibles :
  *   - téléchargement du fichier Parquet brut dans `data/raw/`
  *   - envoi du même fichier dans le bucket RustFS (dossier `nyc_raw/`)
  *   - vérification de la lecture depuis RustFS
  *
  * Usage : sbt "run --nb-months 3"
  */
object Main {
    private val logger = LoggerFactory.getLogger(getClass)
    private val YearMonthFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

    /** Construit une référence [[java.io.File]] pointant vers le répertoire parent (`../`).
    *
    * Préfixe le chemin fourni avec `../` sans vérifier
    * l'existence du fichier ni tester le répertoire courant.
    * Suppose que le programme est lancé depuis `spark_data_integration/`, alors que
    * `.env`, `config.yaml` et `data/` sont à la racine du projet.
    *
    * @param path Le chemin relatif cible depuis le répertoire parent.
    * @return L'objet [[java.io.File]] pointant vers `../<path>`.
    */
    def parentDirectoryFile(path: String): File = new File(s"../$path")

    /** Lit un fichier d'environnement `.env` et injecte ses variables dans la JVM.
    *
    * Analyse le fichier ligne par ligne pour extraire les paires `KEY=VALUE`
    * (en ignorant les lignes vides et les commentaires commençant par `#`).
    * Les valeurs sont nettoyées de leurs guillemets.
    *
    * @param envFileName Nom ou chemin du fichier d'environnement (par défaut ".env").
    */
    def loadEnvFile(envFileName: String = ".env"): Unit = {
        val envFile = parentDirectoryFile(envFileName)
        if (envFile.exists()) {
            Using.resource(Source.fromFile(envFile, "UTF-8")) { source =>
                for (line <- source.getLines()) {
                    val trimmed = line.trim
                    if (trimmed.nonEmpty && !trimmed.startsWith("#") && trimmed.contains("=")) {
                        val Array(key, value) = trimmed.split("=", 2)
                        val k = key.trim
                        val v = value.trim.stripPrefix("\"").stripSuffix("\"")
                        System.setProperty(k, v)
                    }
                }
            }
            logger.info("Fichier .env chargé")
        }
        else{
            logger.error("""Fichier .env non trouvé :
                    |Veuillez créer un fichier .env basé sur .env.example.""".stripMargin)
        }
    }

    /** Charge les identifiants AWS depuis les variables d'environnement depuis le fichier `.env`.
    *
    * @return Le couple (AWS_ACCESS_KEY, AWS_SECRET_KEY)
    */
    def loadEnvironment(): (String, String) = {
        loadEnvFile(".env")

        val accessKey = Option(System.getProperty("AWS_ACCESS_KEY")).getOrElse("")     
        val secretKey = Option(System.getProperty("AWS_SECRET_KEY")).getOrElse("")

        if (accessKey.isEmpty || secretKey.isEmpty) {
            logger.error("Les variables AWS_ACCESS_KEY ou AWS_SECRET_KEY sont introuvables.")
            System.exit(1)
        }
        (accessKey, secretKey)
    }

    /** Charge la configuration de l'application depuis un fichier YAML.
    *
    * Résout et analyse le fichier YAML spécifié en convertissant les structures
    * de données Java sous-jacentes en collections Scala. Interrompt l'application
    * si le fichier est introuvable.
    *
    * @param configFileName Nom ou chemin du fichier de configuration (par défaut "config.yaml").
    * @return Une `Map[String, Any]` représentant la hiérarchie de configuration.
    */
    def loadConfig(configFileName: String = "config.yaml"): Map[String, Any] = {
        val configFile = parentDirectoryFile(configFileName)
        if (!configFile.exists()) {
            logger.error(s"Le fichier de configuration '${configFile.getPath}' n'existe pas.")
            System.exit(1)
        } 
        import scala.jdk.CollectionConverters._
        Using.resource(Source.fromFile(configFile, "UTF-8")) { source =>
            new Yaml().load[JMap[String, Any]](source.reader()).asScala.toMap
        }
    }

    /** Vérifie la disponibilité réseau du service de stockage RustFS (S3).
    *
    * Tente d'établir une connexion HTTP GET rapide vers l'endpoint configuré.
    * Considère le service opérationnel si le code de réponse HTTP est < 500.
    *
    * @param endpoint L'URL de l'endpoint RustFS (ex: "http://localhost:9000").
    * @return `true` si le serveur répond, `false` en cas d'erreur de connexion ou d'erreur serveur (>= 500).
    */
    def checkRustfsConnection(endpoint: String): Boolean = {
        Try {
            val connection = new URL(endpoint).openConnection().asInstanceOf[HttpURLConnection]
            try {
                connection.setRequestMethod("GET")
                connection.setConnectTimeout(2000)
                connection.setReadTimeout(2000)
                connection.getResponseCode < 500
            } finally {
                connection.disconnect()
            }
        } match {
            case Success(true) =>
                logger.info(s"Connexion à RustFS établie sur $endpoint.")
                true
            case _ =>
                logger.error(s"Impossible de joindre le serveur RustFS ($endpoint).")
                false
        }
    }

    /** Vérifie l'existence d'un fichier distant via une requête HTTP HEAD.
    *
    * @param url L'URL complète du fichier distant à vérifier.
    * @return true si le serveur distant répond avec un statut HTTP 200 OK, false sinon.
    */
    def checkRemoteFileExists(url: String): Boolean = {
        Try {
            val connection = new URL(url).openConnection().asInstanceOf[HttpURLConnection]
            connection.setRequestMethod("HEAD")
            connection.setConnectTimeout(5000)
            connection.setReadTimeout(5000)
            connection.getResponseCode == 200
        }.getOrElse(false)
    }

    /** Télécharge un fichier distant vers le stockage local par transfert de flux (streaming).
    *
    * @param url URL du fichier binaire distant à récupérer.
    * @param dest Fichier local [[java.io.File]] de destination.
    * @return `true` si le téléchargement et l'écriture sur disque ont réussi, `false` sinon.
    */
    def downloadToLocal(url: String, dest: File): Boolean = {
        Try {
            val connection = new URL(url).openConnection().asInstanceOf[HttpURLConnection]
            try{
                connection.setConnectTimeout(10000)
                connection.setReadTimeout(60000)
                require(connection.getResponseCode == 200, s"HTTP ${connection.getResponseCode}")
                Using.resource(connection.getInputStream) { in =>
                    Files.copy(in, dest.toPath, StandardCopyOption.REPLACE_EXISTING)
                }

            } finally {
                connection.disconnect()
            }
        } match {
            case Success(_) =>
                logger.info(s" Fichier téléchargé en local : ${dest.getPath}")
                true
            case Failure(e) =>
                logger.error(s"Échec du téléchargement de $url : ${e.getMessage}")
                false
            }
    }

    /** Vérifie l'existence d'un bucket S3 sur RustFS et le crée s'il est absent.
    *
    * Réutilise les identifiants et l'endpoint configurés dans la [[SparkSession]].
    *
    * @param spark  La [[org.apache.spark.sql.SparkSession]] active.
    * @param bucket Le nom du bucket S3 à vérifier ou créer.
    */
    def ensureBucketExists(spark: SparkSession, bucket: String): Unit = {
        val hadoopConf = spark.sparkContext.hadoopConfiguration
        val accessKey = hadoopConf.get("fs.s3a.access.key")
        val secretKey = hadoopConf.get("fs.s3a.secret.key")
        val endpoint = hadoopConf.get("fs.s3a.endpoint")

        val s3 = S3Client.builder()
            .endpointOverride(URI.create(endpoint))
            .region(Region.US_EAST_1)
            .forcePathStyle(true)
            .credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))
        )
        .build()
        try {
            try {
                s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build())
            } catch {
                case _: NoSuchBucketException =>
                    logger.info(s"Création du bucket : '$bucket'")
                    s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build())
            }
        } finally {
            s3.close()
        }
    }

    /** Transfère un fichier binaire local vers RustFS S3 via l'API FileSystem d'Hadoop.
    *
    * Réutilise la configuration Hadoop intégrée à la SparkSession active pour effectuer la copie.
    *
    * @param spark La [[org.apache.spark.sql.SparkSession]] active.
    * @param local Le fichier local [[java.io.File]] à transférer.
    * @param s3Path Le chemin cible S3A (ex: "s3a://nyc-bucket/nyc_raw/file.parquet").
    * @return `true` si l'envoi vers le stockage S3 a réussi, `false` en cas d'échec.
    */
    def uploadToRustfs(spark: SparkSession, local: File, s3Path: String): Boolean = {
        Try {
            val dest = new Path(s3Path)
            val fs = FileSystem.get(dest.toUri, spark.sparkContext.hadoopConfiguration)
            fs.copyFromLocalFile(false, true, new Path(local.toURI), dest)
        } match {
            case Success(_) =>
                logger.info(s" Fichier envoyé vers $s3Path")
                true
            case Failure(e) =>
                logger.error(s"Échec de l'envoi de ${local.getName} vers $s3Path : ${e.getMessage}")
                false
            }
    }

    /** Initialise et configure l'instance unique de [[org.apache.spark.sql.SparkSession]].
    *
    * Définit l'exécution en mode local (`local[*]`) et applique l'ensemble des paramètres
    * Hadoop S3A requis pour communiquer avec l'instance RustFS (endpoint HTTP, identifiants, path-style).
    *
    * @param endpoint URL du service RustFS (ex: "http://localhost:9000").
    * @param accessKey Clé d'accès S3.
    * @param secretKey Clé secrète S3.
    * @return La [[org.apache.spark.sql.SparkSession]] initialisée.
    */
    def createSparkSession(endpoint: String, accessKey: String, secretKey: String): SparkSession = {
        SparkSession.builder()
            .appName("NYC Taxi Ingestion")
            .master("local[*]")
            .config("spark.hadoop.fs.s3a.endpoint", endpoint)
            .config("spark.hadoop.fs.s3a.access.key", accessKey)
            .config("spark.hadoop.fs.s3a.secret.key", secretKey)
            .config("spark.hadoop.fs.s3a.path.style.access", "true")
            .config("spark.hadoop.fs.s3a.connection.ssl.enabled", "false")
            .config("spark.hadoop.fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")
            .getOrCreate()
    }

    /** Orchestre la collecte, la sauvegarde locale, le transfert S3 et la validation des données NYC Taxi.
    *
    * Parcourt les mois récents à la recherche des N jeux de données disponibles, télécharge
    * les fichiers Parquet dans `data/raw/`, les envoie sur le bucket RustFS S3, puis effectue
    * un contrôle d'intégrité final en lisant les données transmises via Spark.
    *
    * @param spark La [[org.apache.spark.sql.SparkSession]] à réutiliser.
    * @param nbMonths Nombre de mois de données récents à collecter.
    * @param configFileName Fichier YAML de configuration (par défaut "config.yaml").
    */
    def fetchRecentNycData(spark: SparkSession,nbMonths: Int,configFileName: String = "config.yaml"): Unit = {

        val config = loadConfig(configFileName)

        val rustfsConfig = config("rustfs").asInstanceOf[JMap[String, Any]]
        val nycConfig = config("nyc_tlc").asInstanceOf[JMap[String, Any]]

        val endpoint = rustfsConfig.get("endpoint").toString
        val bucketName = rustfsConfig.get("bucket_name").toString
        val targetDir = rustfsConfig.get("target_dir").toString
        val baseUrl = nycConfig.get("base_url").toString

        ensureBucketExists(spark, bucketName)

        // Dossier local data/raw/
        val rawDir = parentDirectoryFile("data/raw")
        if (!rawDir.exists()) rawDir.mkdirs()

        var downloadedCount = 0
        var uploadedPaths = List.empty[String]
        val maxSearchDepth = nbMonths + 6
        val currentDate = LocalDate.now()

        logger.info(s"Recherche des $nbMonths derniers mois de données disponibles...")

        for (i <- 0 until maxSearchDepth if downloadedCount < nbMonths) {
            val yearMonth = currentDate.minusMonths(i.toLong).format(YearMonthFormatter)
            val fileUrl = baseUrl.replace("{}", yearMonth)

            if (checkRemoteFileExists(fileUrl)) {
                downloadedCount += 1
                val fileName = s"yellow_tripdata_$yearMonth.parquet"
                val localFile = new File(rawDir, fileName)
                val s3Path = s"s3a://$bucketName/$targetDir/$fileName"

                logger.info(s"[$downloadedCount/$nbMonths] Mois disponible détecté : $yearMonth")

                // Téléchargement local dans data/raw/ et envoi vers RustFS
                if (downloadToLocal(fileUrl, localFile) && uploadToRustfs(spark, localFile, s3Path)) {
                    uploadedPaths ::= s3Path
                }
            }
        }

        if (downloadedCount < nbMonths) {
            logger.warn(s"Seulement $downloadedCount/$nbMonths mois récents ont été trouvés.")
        }

    // Vérification : relecture depuis RustFS avec la même SparkSession
        if (uploadedPaths.nonEmpty) {
            val df = spark.read.parquet(uploadedPaths: _*)
            logger.info(s"Vérification RustFS : ${df.count()} lignes dans ${uploadedPaths.size} fichier(s).")
            df.printSchema()
            logger.info(s"Terminé : ${uploadedPaths.size} mois chargés dans data/raw/ et sur RustFS.")
        }
    }

    /** Analyse et extrait le paramètre CLI fixant le nombre de mois à traiter.
    *
    * Inspecte les arguments du tableau à la recherche de `--nb_months`, `--nb-months` ou `-n`.
    *
    * @param args Tableau des arguments transmis à l'application.
    * @return Le nombre de mois à traiter sous forme d'entier (3 par défaut si non spécifié ou -1 si invalide).
    */
    def parseArgs(args: Array[String]): Int = {
        args.sliding(2, 1).collect {
            case Array("--nb_months", value) => Try(value.toInt).getOrElse(-1)
            case Array("--nb-months", value) => Try(value.toInt).getOrElse(-1)
            case Array("-n", value)          => Try(value.toInt).getOrElse(-1)
        }.toList.headOption.getOrElse(3)
    }

    def main(args: Array[String]): Unit = {
        val nbMonths = parseArgs(args)

        if (nbMonths < 1) {
            logger.error("Le nombre de mois doit être un entier supérieur ou égal à 1.")
            System.exit(1)
        }

        val (accessKey, secretKey) = loadEnvironment()
        val config = loadConfig()
        val endpoint = config("rustfs").asInstanceOf[JMap[String, Any]].get("endpoint").toString

        if (!checkRustfsConnection(endpoint)) {
            logger.error("Traitement interrompu : RustFS est inaccessible (docker compose up ?).")
            System.exit(1)
        }

        val spark = createSparkSession(endpoint, accessKey, secretKey)

        try {
            fetchRecentNycData(spark, nbMonths)
        } finally {
            logger.info("Fermeture de la SparkSession.")
            spark.stop()
        }
    }



}
