package fr.cytech.integration

// Fonctions spark : spark sessions, spark SQL
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}
import org.apache.hadoop.fs.{FileSystem, Path}

import org.yaml.snakeyaml.Yaml // Bibliothèque pour lire un fichier yaml facilement

// Sert a la conversion vers des chaines de charactères
import java.io.FileInputStream
import java.util.{Map => JMap}

import scala.jdk.CollectionConverters._
import io.github.cdimascio.dotenv.Dotenv // équivalent de la bibliothèque Dotenv de python
import fr.cytech.integration.utils._
import scopt.OParser

import java.net.URI

import fr.cytech.integration.data_processing.normalization._ // import des classes de traitement des données
import fr.cytech.integration.data_processing.validation._ // import des classes de traitement des données

/**
 * Structure recevant les arguments de la ligne de commande.
 */
case class CliArgs(
                    configPath: String = "default_config.yaml",
                    envPath: String = ".env",
                    verbose: String = "M"
                  )

/**
 * Classe principal.
 * Elle est le point d'entrée de l'exercice sur le traitement et la validation des données.
 * Pour changer sa configuration, il faut changer le default_config.yaml ou lui donnée une configuration personalisé en lui donnant un config.yaml
 */
object Main extends AppLogger {

  def main(args: Array[String]): Unit = {
    val builder = OParser.builder[CliArgs]
    val parser = {
      import builder._
      OParser.sequence(
        programName("nettoyage-validation-exo2"),
        head("Nettoyage et Validation des données", "1.0"),

        opt[String]('c', "config")
          .valueName("<fichier>")
          .action((x, c) => c.copy(configPath = x))
          .text("Chemin vers le fichier YAML (défaut: config.yaml)"),

        opt[String]('e', "env")
          .valueName("<fichier>")
          .action((x, c) => c.copy(envPath = x))
          .text("Chemin vers le fichier .env (défaut: .env local)"),

        opt[String]('v', "verbose")
          .action((x, c) => c.copy(verbose = x))
          .text("Active les logs de débogage (peut mener a des performances dégradées en raison des prints incessants"),

        help("help").text("Montre l'aide")
      )
    }
    // Exécution du parsing
    OParser.parse(parser, args, CliArgs()) match {
      case Some(cli) =>
        Config.init(cli.configPath) // initialisation de Config
        LogConfig.setVerbosity(cli.verbose)

        ready(cli.envPath)
      case _ =>
        sys.exit(1)
    }
  }

  /**
   * Lecture du fichier .env et redirection vers les valeurs par défaut si les clés ne sont pas trouvées.
   *
   * @param dotenv_path Chemin vers le .env
   * @return clés d'accès
   */
  def readDotEnv(dotenv_path: String): Map[String, String] = {
    val dotenv: Dotenv = Dotenv
      .configure()
      .directory(".")
      .filename(".env")
      .ignoreIfMissing()
      .load()

    // Cherche d'abord dans le .env, puis dans les variables système (sys.env)
    val accessKey = Option(dotenv.get("S3_ACCESS_KEY"))
      .getOrElse(throw new RuntimeException("Variable S3_ACCESS_KEY manquante"))

    val secretKey = Option(dotenv.get("S3_SECRET_KEY"))
      .getOrElse(throw new RuntimeException("Variable S3_SECRET_KEY manquante"))

    Map("accessKey" -> accessKey, "secretKey" -> secretKey)
  }

  /**
   * Fonction qui vérifie l'existence d'un certain bucket.
   * Si il n'existe pas : il est créer à la volé
   *
   * @param spark      la session spark acutel
   * @param bucketName Nom du bucket
   */
  def ensureBucketExists(spark: SparkSession, bucketName: String): Unit = {
    val bucketUri = new URI(Config.getSetting("nyc_bucket").asString + bucketName)
    val hadoopConf = spark.sparkContext.hadoopConfiguration

    // Récupère l'implémentation S3AFileSystem configurée dans Spark
    val fs = FileSystem.get(bucketUri, hadoopConf)
    val bucketPath = new Path(bucketUri)

    if (!fs.exists(bucketPath)) {
      log(LogLevel.Info, LogScope.Detail)(s"Le bucket '$bucketName' n'existe pas. Création en cours...")
      val created = fs.mkdirs(bucketPath)
      if (created) {
        log(LogLevel.Info, LogScope.Detail)(s"Bucket '$bucketName' créé avec succès.")
      } else {
        throw new RuntimeException(s"Échec de la création du bucket '$bucketName' sur RustFS.")
      }
    } else {
      log(LogLevel.Info, LogScope.Detail)(s"Le bucket '$bucketName' existe déjà.")
    }
  }

  /**
   * Focntion utilitaire de validation des parquets. Cela permet d'évité les erreurs de type "File not exists"
   *
   * @param spark     session spark
   * @param rawPath   chemin vers les fichiers
   * @param fileNames nom des fichiers
   * @return la liste des URL entière des fichiers trouvés sur le bucket s3
   */
  def validateParquetFiles(
                            spark: SparkSession,
                            rawPath: String,
                            fileNames: List[String]
                          ): List[String] = {
    val hadoopConf = spark.sparkContext.hadoopConfiguration
    val baseUri = new URI(rawPath)
    val fs = FileSystem.get(baseUri, hadoopConf)

    // Nettoie le slash final éventuel sur rawPath (ex: "s3a://nyc-bucket/nyc_raw")
    val cleanBasePath = rawPath.stripSuffix("/")

    val (existing, missing) = fileNames.partition { fileName =>
      val fullPath = new Path(s"$cleanBasePath/$fileName")
      fs.exists(fullPath) && fs.isFile(fullPath)
    }

    if (missing.nonEmpty) {
      log(LogLevel.Error, LogScope.Lifecycle)(s"Fichiers Parquet introuvables sur RustFS : ${missing.mkString(", ")}")
      throw new java.io.FileNotFoundException(
        s"Fichiers manquants dans '$cleanBasePath' : ${missing.mkString(", ")}"
      )
    }

    log(LogLevel.Info, LogScope.Detail)(s"${existing.size} fichier(s) Parquet vérifié(s) avec succès sur RustFS.")
    existing.map(fileName => s"$cleanBasePath/$fileName")
  }

  /**
   * Mise en place des variables globales et démarrage du job spark
   *
   * @param dotenv_path chemin vers le fichier .env à utiliser
   */
  def ready(dotenv_path: String): Unit = {
    if (Config.getSetting("local_copy").asBoolean) {
      // Créer un dossier de quarantine si nécessaire
      if (Config.getSetting("quarantine").asBoolean) {
        val quarantine_path: String = Config.getSetting("quarantine_path").asString
        LocalFilesController.createFolder(LocalFilesController.resolvePath(Config.getSetting("quarantine_local_path").asString).toString)
      }

      // Créer le dossier de stockage locale si nécessaire
      LocalFilesController.createFolder(LocalFilesController.resolvePath(Config.getSetting("processed_local_path").asString).toString)
    }

    var accessKeySecretKey = readDotEnv(dotenv_path)

    log(LogLevel.Info, LogScope.Step)("Démarrage de la session Spark")

    try {
      val spark = SparkSession.builder()
        .appName("nyc-taxi-integration")
        .master(Config.getSetting("master").asString)
        .config("spark.hadoop.fs.s3a.endpoint", Config.getSetting("endpoint").asString)
        .config("spark.hadoop.fs.s3a.access.key", accessKeySecretKey("accessKey"))
        .config("spark.hadoop.fs.s3a.secret.key", accessKeySecretKey("secretKey"))
        .config("spark.hadoop.fs.s3a.path.style.access", "true")
        .config("spark.sql.debug.maxToStringFields", "200")
        .getOrCreate()

      // s'assure que le bucket existe
      ensureBucketExists(spark, Config.getSetting("processed_path").asString)

      // Réccupère les parquets et les fusionne en un dataframe
      val parquets: List[String] = Config.getSetting("parquets").asStringList
      log(LogLevel.Info, LogScope.Detail)("Parquets : " + parquets)

      val valid_parquets = validateParquetFiles(spark, Config.getSetting("nyc_bucket").asString + Config.getSetting("raw_path").asString, parquets)

      val raw = spark.read.parquet(valid_parquets: _*).withColumn("source_month", regexp_extract(col("_metadata.file_name"), "_(\\d{4}-\\d{2})\\.parquet", 1))

      log(LogLevel.Info, LogScope.Detail)(s"Dataframe chargé : contient ${raw.count()} lignes")
      pipeline(spark, raw)
    } catch {
      case e: Exception =>
        log(LogLevel.Error, LogScope.Lifecycle)(s"Une erreur s'est produite : ${e}")
    }
  }

  /**
   * Pipeline complète de traitement des données : normalisation, validation, enregistrement locaux et distant
   *
   * @param spark         session spark actuel (une seul peux existé à la fois sur une machine)
   * @param raw_dataframe le dataframe de départ
   */
  def pipeline(spark: SparkSession, raw_dataframe: DataFrame): Unit = {
    log(LogLevel.Info, LogScope.Lifecycle)("Nettoyage des données")

    log(LogLevel.Info, LogScope.Step)("Normalisation des données")
    val normalized_df: DataFrame = Normalize.normalize(raw_dataframe)

    log(LogLevel.Info, LogScope.Step)("Validation des données")
    // sert au calcul du dataframe de quarantaine
    val identified_df: DataFrame = normalized_df
      .withColumn("row_id", monotonically_increasing_id())
      .persist() // évite que les ids changent si le plan est recalculé

    val validated_df: DataFrame = Validation.filters(identified_df)

    // Dataframe de quarantine
    if (Config.getSetting("quarantine").asBoolean) {
      log(LogLevel.Info, LogScope.Detail)("Création du dataframe de quarantaine")
      val quarantine_df: DataFrame = identified_df.join(
        validated_df.select("row_id"),
        Seq("row_id"),
        "left_anti"
      )
      log(LogLevel.Info, LogScope.Detail)(s"Le dataframe de quarantaine contient ${quarantine_df.count()} lignes, soit ${(quarantine_df.count() / raw_dataframe.count()) * 100}% des données brutes")
      LocalFilesController.saveParquet(quarantine_df, LocalFilesController.resolvePath(Config.getSetting("quarantine_local_path").asString).toString)

      log(LogLevel.Info, LogScope.Step)(s"Enregistrement du dataframe quarantaine dans le stockage RustFS")
      quarantine_df.write
        .mode(SaveMode.Overwrite)
        .parquet(Config.getSetting("nyc_bucket").asString + Config.getSetting("quarantine_path").asString)
    }
    log(LogLevel.Info, LogScope.Step)("Processus terminé : enregistrement des données")
    log(LogLevel.Debug, LogScope.Detail)(s"Dataframe final : contient ${validated_df.count()} lignes")

    // Copie locale des fichiers
    LocalFilesController.saveParquet(validated_df, LocalFilesController.resolvePath(Config.getSetting("processed_local_path").asString).toString)

    log(LogLevel.Info, LogScope.Step)(s"Enregistrement du dataframe validé dans le stockage RustFS")
    validated_df.write
      .mode(SaveMode.Overwrite)
      .parquet(Config.getSetting("nyc_bucket").asString + Config.getSetting("processed_path").asString)

    log(LogLevel.Info, LogScope.Lifecycle)("Processus terminé")
  }
}