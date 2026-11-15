package fr.cytech.integration

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

import org.yaml.snakeyaml.Yaml
import java.io.FileInputStream
import java.util.{Map => JMap}
import scala.jdk.CollectionConverters._ // ._ en Scala 2.13
import io.github.cdimascio.dotenv.Dotenv

/**
 * La quarantaine est un ensemble de sous dataframe du dataframe initial.
 * Ces dataframe regroupe toutes les valeurs qui on été altéré (normalize) ou retiré (validiation).
 * L'objectif est de garder une trace de ces données si des informations supplémentaires les rendent exploitables / à des fins de débug.
 *
 * @param enabled Créer des fcihiers de quarantaine
 * @param path_normalized_rows
 * @param path_unvalidated_rows
 */
case class QuarantineConfig(enabled: Boolean = false, path_normalized_rows: String = "", path_unvalidated_rows: String = "")

object Main {
  def main(args: Array[String]): Unit = {

    val yaml = new Yaml()
    val input = getClass.getClassLoader.getResourceAsStream("config.yaml")
    val config = yaml.load[JMap[String, Any]](input).asScala
    input.close()

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
    //raw.printSchema()
  }
}