package fr.cytech.integration

// Fonctions spark : spark sessions, spark SQL
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

import org.yaml.snakeyaml.Yaml // Bibliothèque pour lire un fichier yaml facilement

// Sert a la conversion vers des chaines de charactères
import java.io.FileInputStream
import java.util.{Map => JMap}

import scala.jdk.CollectionConverters._
import io.github.cdimascio.dotenv.Dotenv // équivalent de la bibliothèque Dotenv de python

/**
 * La quarantaine est un sous ensemble du dataframe initial.
 * Ces dataframe regroupe toutes les valeurs qui on été altéré (normalize) ou retiré (validiation).
 * L'objectif est de garder une trace de ces données si des informations supplémentaires les rendent exploitables / à des fins de débug.
 *
 * @param enabled Créer des fcihiers de quarantaine
 * @param path_normalized_rows
 * @param path_unvalidated_rows
 */
case class QuarantineConfig(enabled: Boolean = false, path_normalized_rows: String = "", path_unvalidated_rows: String = "")

/**
 * Classe principal.
 * Elle est le point d'entrée de l'exercice sur le traitement et la validation des données.
 * Pour changer sa configuration, il faut changer le default_config.yaml ou lui donnée une configuration personalisé en lui donnant un config.yaml
 */
object Main {
  def main(args: Array[String]): Unit = {

    val yaml = new Yaml()
    val input = getClass.getClassLoader.getResourceAsStream("config.yaml")
    val config = yaml.load[JMap[String, Option[Double]](input).asScala
    input.close()

    val abberation_filters_tresholds = config("filters_thresholds").asInstanceOf[JMap[String, Any]].asScala
    val app_config = config("app").asInstanceOf[JMap[String, Any]].asScala
    val sparkConf = app_config("spark").asInstanceOf[JMap[String, Any]].asScala
    val s3Conf = app_config("s3").asInstanceOf[JMap[String, Any]].asScala

    val dotenv = Dotenv.configure().ignoreIfMissing().load()

    // Cherche d'abord dans le .env, puis dans les variables système (sys.env)
    val accessKey = Option(dotenv.get("S3_ACCESS_KEY"))
      .getOrElse(throw new RuntimeException("Variable S3_ACCESS_KEY manquante"))

    val secretKey = Option(dotenv.get("S3_SECRET_KEY"))
      .getOrElse(throw new RuntimeException("Variable S3_SECRET_KEY manquante"))


    val spark = SparkSession.builder()
      .appName("nyc-taxi-integration")
      .master(sparkConf("master").asInstanceOf[String])
      .config("spark.hadoop.fs.s3a.endpoint", s3Conf("endpoint").asInstanceOf[String])
      .config("spark.hadoop.fs.s3a.access.key", accessKey)
      .config("spark.hadoop.fs.s3a.secret.key", secretKey)
      .config("spark.hadoop.fs.s3a.path.style.access", "true")
      .getOrCreate()

    val raw = spark.read.parquet(s3Conf("parquet_path").asInstanceOf[String])
  }
}