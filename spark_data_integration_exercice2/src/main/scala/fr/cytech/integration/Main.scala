package fr.cytech.integration

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

object Main {
  def main(args: Array[String]): Unit = {
    println("Hello World")

    val spark = SparkSession.builder()
      .appName("nyc-taxi-integration")
      .master("local[*]") // local : utilise tous les coeurs de la machine
      .config("spark.hadoop.fs.s3a.endpoint", "http://localhost:9000")
      .config("spark.hadoop.fs.s3a.access.key", "rustfsadmin")
      .config("spark.hadoop.fs.s3a.secret.key", "rustfsadmin")
      .config("spark.hadoop.fs.s3a.path.style.access", "true")
      .getOrCreate()

    val raw = spark.read.parquet("s3a://nyc-bucket/nyc_raw/yellow_tripdata_2026-07.parquet")
    raw.printSchema()
    raw.describe("trip_distance", "total_amount").show()

    raw.groupBy("RatecodeID").count().orderBy("RatecodeID").show()

    val nullCounts = raw.select(
      raw.columns.map(c => count(when(col(c).isNull, 1)).alias(c)): _*
    )
    nullCounts.show(false)
  }

}