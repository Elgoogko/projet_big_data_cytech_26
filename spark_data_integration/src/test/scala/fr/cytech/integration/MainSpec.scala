package fr.cytech.integration

import com.sun.net.httpserver.{HttpExchange, HttpServer}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.io.File
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.{Map => JMap}

/** Tests unitaires de [[Main]].
  *
  * Les fonctions dépendant de Spark et de RustFS (`ensureBucketExists`, `uploadToRustfs`,
  * `createSparkSession`, `fetchRecentNycData`) relèvent de tests d'intégration et ne sont pas couvertes ici.
  * À lancer depuis `spark_data_integration/` : `sbt test`.
  */
class MainSpec extends AnyFlatSpec with Matchers {

    /** Démarre un serveur HTTP local éphémère sur `127.0.0.1` pour simuler des endpoints distants.
      *
      * Alloue automatiquement un port libre, associe le gestionnaire fourni à la racine (`/`)
      * et arrête systématiquement le serveur à l'issue de l'exécution du bloc de test.
      *
      * @param handler  Fonction de traitement des requêtes HTTP entrantes ([[com.sun.net.httpserver.HttpExchange]]).
      * @param testCode Bloc de test recevant en paramètre l'URL de base du serveur (ex: `"http://127.0.0.1:54321"`).
      */
    private def withMockHttpServer(handler: HttpExchange => Unit)(testCode: String => Unit): Unit = {
        val server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/", (exchange: HttpExchange) => handler(exchange))
        server.start()
        val baseUrl = s"http://127.0.0.1:${server.getAddress.getPort}"
        try {
            testCode(baseUrl)
        } finally {
            server.stop(0)
        }
    }

    /** Crée un fichier temporaire dans le répertoire parent (`../`) le temps d'un test.
      *
      * Écrit le contenu spécifié en UTF-8 dans le fichier créé, passe uniquement son nom
      * au bloc de test (pour compatibilité avec `Main.parentDirectoryFile`), puis supprime
      * le fichier dans un bloc `finally`.
      *
      * @param prefix   Préfixe du nom du fichier temporaire.
      * @param suffix   Suffixe ou extension du fichier temporaire (ex: .env ou .yaml).
      * @param content  Contenu textuel à écrire dans le fichier en UTF-8.
      * @param testCode Bloc de test recevant en paramètre le nom du fichier créé.
      */
    private def withParentTempFile(prefix: String, suffix: String, content: String)(testCode: String => Unit): Unit = {
        val parentDir = new File("..").getCanonicalFile
        val tempFile = Files.createTempFile(parentDir.toPath, prefix, suffix).toFile
        try {
            Files.write(tempFile.toPath, content.getBytes(StandardCharsets.UTF_8))
            testCode(tempFile.getName)
        } finally {
            tempFile.delete()
        }
    }

    // Test parseArgs
    "parseArgs" should "retourner Right(3) par défaut si aucun argument n'est fourni" in {
        Main.parseArgs(Array.empty) shouldBe Right(3)
    }

    it should "extraire le nombre de mois avec --nb_months, --nb-months ou -n" in {
        Main.parseArgs(Array("--nb_months", "5")) shouldBe Right(5)
        Main.parseArgs(Array("--nb-months", "12")) shouldBe Right(12)
        Main.parseArgs(Array("-n", "1")) shouldBe Right(1)
    }

    it should "retourner Left si les arguments sont invalides ou incomplets" in {
        Main.parseArgs(Array("--nb-months", "abc")).isLeft shouldBe true
        Main.parseArgs(Array("-n", "0")).isLeft shouldBe true
        Main.parseArgs(Array("--nb-months", "-2")).isLeft shouldBe true
        Main.parseArgs(Array("--nb-month", "5")).isLeft shouldBe true
        Main.parseArgs(Array("-n")).isLeft shouldBe true
    }

    // Test parentDirectoryFile

    "parentDirectoryFile" should "préfixer le chemin avec ../" in {
        Main.parentDirectoryFile("data/raw").getPath shouldBe "../data/raw"
        Main.parentDirectoryFile(".env").getPath shouldBe "../.env"
    }

    // Test loadEnvFile 

    "loadEnvFile" should "charger les paires KEY=VALUE, ignorer les commentaires et retirer les guillemets" in {
        val envContent =
            """# Commentaire ignoré
            |
            |TEST_AWS_KEY=my-access-key
            |TEST_AWS_SECRET="my-secret-key"
            |  TEST_SPACED_KEY =  "value=with=equals"
            |""".stripMargin

        try {
            withParentTempFile("test-", ".env", envContent) { fileName =>
                Main.loadEnvFile(fileName)

                System.getProperty("TEST_AWS_KEY") shouldBe "my-access-key"
                System.getProperty("TEST_AWS_SECRET") shouldBe "my-secret-key"
                System.getProperty("TEST_SPACED_KEY") shouldBe "value=with=equals"
            }
        } finally {
            System.clearProperty("TEST_AWS_KEY")
            System.clearProperty("TEST_AWS_SECRET")
            System.clearProperty("TEST_SPACED_KEY")
        }
    }

    it should "ne pas définir de variable pour une ligne de commentaire" in {
        try {
            withParentTempFile("test-", ".env", "# TEST_COMMENTED=1\n") { fileName =>
                Main.loadEnvFile(fileName)
                System.getProperty("TEST_COMMENTED") shouldBe null
            }
        } finally {
            System.clearProperty("TEST_COMMENTED")
        }
    }


    // Test loadConfig

    "loadConfig" should "charger et parser correctement un fichier YAML" in {
        val yamlContent =
            """rustfs:
              |  endpoint: "http://localhost:9000"
              |  bucket_name: "nyc-bucket"
              |  target_dir: "nyc_raw"
              |nyc_tlc:
              |  base_url: "https://example.com/yellow_tripdata_{}.parquet"
              |""".stripMargin

        // Écrit ce YAML dans un fichier temporaire dans ../
        withParentTempFile("test-config-", ".yaml", yamlContent) { fileName =>
            val config = Main.loadConfig(fileName)

            config should contain key "rustfs"
            config should contain key "nyc_tlc"

            val rustfs = config("rustfs").asInstanceOf[JMap[String, Any]]
            rustfs.get("endpoint") shouldBe "http://localhost:9000"
            rustfs.get("bucket_name") shouldBe "nyc-bucket"
            rustfs.get("target_dir") shouldBe "nyc_raw"

            val tlc = config("nyc_tlc").asInstanceOf[JMap[String, Any]]
            tlc.get("base_url") shouldBe "https://example.com/yellow_tripdata_{}.parquet"
        }
    }

    // Test checkRustfsConnection

    "checkRustfsConnection" should "retourner true si le serveur répond avec un code < 500" in {
        withMockHttpServer { exchange =>
            exchange.sendResponseHeaders(403, -1)
            exchange.close()
        } { url =>
            Main.checkRustfsConnection(url) shouldBe true
        }
    }

    it should "retourner false si le serveur renvoie une erreur 500" in {
        withMockHttpServer { exchange =>
            exchange.sendResponseHeaders(500, -1)
            exchange.close()
        } { url =>
            Main.checkRustfsConnection(url) shouldBe false
        }
    }

    it should "retourner false si le serveur est injoignable" in {
        Main.checkRustfsConnection("http://127.0.0.1:59999") shouldBe false
    }

    // Test checkRemoteFileExists 

    "checkRemoteFileExists" should "retourner true pour un statut HTTP 200 et false pour 404" in {
        withMockHttpServer { exchange =>
            val status = if (exchange.getRequestURI.getPath == "/exists.parquet") 200 else 404
            exchange.sendResponseHeaders(status, -1)
            exchange.close()
        } { baseUrl =>
            Main.checkRemoteFileExists(s"$baseUrl/exists.parquet") shouldBe true
            Main.checkRemoteFileExists(s"$baseUrl/missing.parquet") shouldBe false
        }
    }

    it should "retourner false si le serveur est injoignable" in {
        // Appelle un port fermé et vérifie que ça retourne false.
        Main.checkRemoteFileExists("http://127.0.0.1:59999/file.parquet") shouldBe false
    }

    // Test downloadToLocal

    "downloadToLocal" should "télécharger le contenu distant dans le fichier local" in {
        // Crée un tableau d'octets simulant le contenu d'un fichier Parquet
        val payload = "contenu-parquet-simule".getBytes(StandardCharsets.UTF_8)

        withMockHttpServer { exchange =>
            // Configure le serveur simulé pour servir ces octets sur /data.parquet.
            if (exchange.getRequestURI.getPath == "/data.parquet") {
                // Annonce un statut 200 OK et la taille exacte des données à transférer.
                exchange.sendResponseHeaders(200, payload.length.toLong)
                // Écrit les octets dans le flux de sortie HTTP puis le ferme pour finaliser l'envoi.
                val os = exchange.getResponseBody
                os.write(payload)
                os.close()
            } else {
                exchange.sendResponseHeaders(404, -1)
                exchange.close()
            }
        } { baseUrl =>
            // Crée un fichier vide temporaire sur le disque local qui recevra le téléchargement.
            val destFile = Files.createTempFile("download-test-", ".parquet").toFile
            try {
                Main.downloadToLocal(s"$baseUrl/data.parquet", destFile) shouldBe true
                // Relit les octets écrits sur le disque et vérifie qu'ils sont strictement identiques aux données envoyées par le serveur.
                Files.readAllBytes(destFile.toPath) shouldBe payload
            } finally {
                destFile.delete()
            }
        }
    }

    it should "retourner false si le fichier distant est absent (404)" in {
        withMockHttpServer { exchange =>
            exchange.sendResponseHeaders(404, -1)
            exchange.close()
        } { baseUrl =>
            val destFile = Files.createTempFile("download-test-", ".parquet").toFile
            try {
                Main.downloadToLocal(s"$baseUrl/absent.parquet", destFile) shouldBe false
            } finally {
                destFile.delete()
            }
        }
    }
}