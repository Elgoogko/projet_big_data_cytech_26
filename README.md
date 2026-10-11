# 💻 Projet Big Data

## Descriptif

### Modalités de rendu

1. Pull Request vers la branch `main`
2. Dépot du rapport et du code source zippés dans Teams (ouverture prochaine des accès)

Date limite de rendu : Lundi 21 décembre 2026 à 6h00m00s

## Environment

- Java 11
- Python 3.10
- Scala 2.13.17
- RustFS


Le code minimal pour faire fonctionner un code avec RustFS :
```scala
import org.apache.spark.sql.{SparkSession, DataFrame}

object SparkApp extends App {
  val spark = SparkSession.builder()
    .appName("SparkApp")
    .master("local")
    .config("fs.s3a.access.key", "rustfsadmin")
    .config("fs.s3a.secret.key", "rustfsadmin")
    .config("fs.s3a.endpoint", "http://localhost:9000/") // A changer lors du déploiement
    .config("fs.s3a.path.style.access", "true")
    .config("fs.s3a.connection.ssl.enable", "false")
    .config("fs.s3a.attempts.maximum", "1")
    .config("fs.s3a.connection.establish.timeout", "6000")
    .config("fs.s3a.connection.timeout", "5000")
    .getOrCreate()
  spark.sparkContext.setLogLevel("WARN")

}
```

En cas de développement de composants en langage Python pour l'exercice 4 et exercice 5, l'utilisation d'UV est
obligatoire.

Si vous faites un notebook exploratoire de données, je vous demande de le faire via Marimo et non via Jupyter.