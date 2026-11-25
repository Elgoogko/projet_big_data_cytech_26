package fr.cytech.fixtures.configs_yaml

object ConfigDatas {
  val defaultConfig: Map[String, Any] = Map(
    "fare_amount" -> null,
    "extra" -> null,
    "mta_tax" -> null,
    "tip_amount" -> null,
    "tolls_amount" -> null,
    "improvement_surcharge" -> null,
    "total_amount" -> null,
    "congestion_surcharge" -> null,
    "Airport_fee" -> null,
    "cbd_congestion_fee" -> null,
    "app_name" -> "spark_data_integration_exercice2",
    "master" -> "local[*]",
    "endpoint" -> "http://localhost:9000",
    "raw_path" -> "s3a://nyc-bucket/nyc_raw",
    "parquets" -> List("yellow_tripdata_2026-07.parquet"),
    "quarantine" -> true,
    "quarantine_path" -> "data/quarantine"
  )

  val validConfig: Map[String, Any] = Map(
    "fare_amount" -> 0.0,
    "extra" -> 0.0,
    "mta_tax" -> 0.0,
    "tip_amount" -> 0.0,
    "tolls_amount" -> 0.0,
    "improvement_surcharge" -> 0.0,
    "total_amount" -> 0.0,
    "congestion_surcharge" -> 0.0,
    "Airport_fee" -> 0.0,
    "cbd_congestion_fee" -> 0.0,
    "app_name" -> "spark_data_integration_exercice2",
    "master" -> "local[*]",
    "endpoint" -> "http://localhost:9001",
    "raw_path" -> "s3a://nyc-bucket/nyc_raw",
    "parquets" -> List("yellow_tripdata_2026-06.parquet", "yellow_tripdata_2026-07.parquet"),
    "quarantine" -> true,
    "quarantine_path" -> "data/quarantine"
  )

  val configsListYAML = List("src/test/scala/fr/cytech/fixtures/configs_yaml/valid_config.yaml", "src/test/scala/fr/cytech/fixtures/configs_yaml/invalid_config.yaml")
}