# Exercice 2 : Validation des données via Spark

L'objectif de cet exercice est de valider les données extraites du parquet du DataLake.
C'est un premier nettoyage des données.

# Pipeline de traitement

|                      | Données brutes                                             | Normalisation                                                     | Validation                                                                                                                                             | Sauvegarde                                              |
|----------------------|------------------------------------------------------------|-------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------|
| Type de données      | Parquet                                                    | DataFrame                                                         | DataFrame                                                                                                                                              | Parquet                                                 |
| Action               | Chargement des données depuis le bucket RustFS             | Transformation des données (Null en codes). Altération du dataset | Validation des données selon le contrat donnés. En deux étapes: 1) Filtre par rapport au contrat. 2) Filtre de cohérence (valeurs abbérantes, dates..) | sauvegarde des données prêtes à l'emploi dans le bucket |
| Chemin de sauvegarde | "s3a://nyc-bucket/nyc_raw/yellow_tripdata_2026-07.parquet" | data/interim/quarantine                                           | data/interim/quarantine                                                                                                                                | data/processed                                          |

# TODO

## Liste Branche 1 : traitement des données

- [x] Créer la couche de filtrage de normalisation des données et ses tests associés
- [x] Créer la couche de filtrage de validation des données #1, filtrage par rapport au contrat et ses tests associés
- [ ] Créer le script de validation du fichier de configuration et des valeurs de fallbacks
- [ ] Créer la couche de filtrage de validation des données #2, filtrage de cohérence (valeurs abbérantes, dates..) et
  ses tests associés
- [ ] Ajout d'un "verbose_mode" pour afficher les données altérées et retirées
- [ ] Ajout de la "quarantine" pour sauvegarder les données altérées et retirées
- [ ] Script de création d'un bucket RustFS "nyc_processed"
- [ ] Data exploration pour déterminer le seuil d'acceptabilité des données
- [ ] Création du squelette de la branche 2

## Liste branche 2 : intégration dans la bdd SQL

# Lecture des données

root
|-- VendorID: integer (nullable = true)
|-- tpep_pickup_datetime: timestamp_ntz (nullable = true)
|-- tpep_dropoff_datetime: timestamp_ntz (nullable = true)
|-- passenger_count: long (nullable = true)
|-- trip_distance: double (nullable = true)
|-- RatecodeID: long (nullable = true)
|-- store_and_fwd_flag: string (nullable = true)
|-- PULocationID: integer (nullable = true)
|-- DOLocationID: integer (nullable = true)
|-- payment_type: long (nullable = true)
|-- fare_amount: double (nullable = true)
|-- extra: double (nullable = true)
|-- mta_tax: double (nullable = true)
|-- tip_amount: double (nullable = true)
|-- tolls_amount: double (nullable = true)
|-- improvement_surcharge: double (nullable = true)
|-- total_amount: double (nullable = true)
|-- congestion_surcharge: double (nullable = true)
|-- Airport_fee: double (nullable = true)
|-- cbd_congestion_fee: double (nullable = true)
|-- request_source: string (nullable = true)

+-------+-----------------+------------------+
|summary| trip_distance| total_amount|
+-------+-----------------+------------------+
| count| 3530109| 3530109|
| mean|5.547543863375621|30.043324313784044|
| stddev|576.2019217068215|23.157295991691058|
| min| 0.0| -981.0|
| max| 318129.1| 6471.35|
+-------+-----------------+------------------+

# Précision des fonctions

## Le mot clé "Implicit"

On se sert de "implicit" dans ce type de code :

```scala
// Création de la session spark
  implicit val spark: SparkSession = SparkSession.builder()
    .appName("FilterTest")
    .master("local[*]")
    .getOrCreate()

  import spark.implicits._
  spark.sparkContext.setLogLevel("ERROR")

  override def afterAll(): Unit = {
    spark.stop()
    super.afterAll()
  }
```

Une valeur de type "implicit" est une valeur qui peut être utilisée sans avoir besoin de la déclarer explicitement. Elle
est automatiquement disponible dans le contexte courant. Dans le code ci-dessus, "spark" est une valeur de type
"implicit" qui est utilisée dans le code sans avoir besoin de la déclarer explicitement. Ainsi, toute fonction
"Inconnue" se soumet directement à la variable implicite.
Exemple :
La fonction "toDF" n'existe pas directement sur le type "Seq", mais elle est disponible grâce à l'import implicite de
"spark.implicits._".

# Sources

- [écrire des tests scala](https://docs.scala-lang.org/toolkit/testing-suite.html)
- [Spark SQL](https://spark.apache.org/docs/latest/sql-programming-guide.html)