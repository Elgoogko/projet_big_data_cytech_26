ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.18"

lazy val root = (project in file("."))
  .settings(
    name := "exo2_data_ingestion"
  )

// https://mvnrepository.com/artifact/org.apache.spark/spark-core
libraryDependencies += "org.apache.spark" %% "spark-core" % "4.2.0"
// https://mvnrepository.com/artifact/org.apache.spark/spark-sql
libraryDependencies += "org.apache.spark" %% "spark-sql" % "4.2.0"
// Source: https://mvnrepository.com/artifact/org.apache.spark/spark-hadoop-cloud
libraryDependencies += "org.apache.spark" %% "spark-hadoop-cloud" % "4.2.0"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % Test

// Lecture d'un fichier YAML de configuration
libraryDependencies += "org.yaml" % "snakeyaml" % "2.3"

// lecture simplifié des .env (pour java mais fonctionne aussi avec scala)
libraryDependencies += "io.github.cdimascio" % "dotenv-java" % "3.0.0"

// bibliothèque d'argparser pour sbt
libraryDependencies += "com.github.scopt" %% "scopt" % "4.1.0"

Test / fork := true
Test / javaOptions += "--add-exports=java.base/sun.nio.ch=ALL-UNNAMED"

fork := true
javaOptions ++= Seq(
  "--add-opens=java.base/java.lang=ALL-UNNAMED",
  "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
  "--add-opens=java.base/java.io=ALL-UNNAMED",
  "--add-opens=java.base/java.net=ALL-UNNAMED",
  "--add-opens=java.base/java.nio=ALL-UNNAMED",
  "--add-opens=java.base/java.util=ALL-UNNAMED",
  "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
  "--add-opens=java.base/sun.nio.cs=ALL-UNNAMED",
  "--add-opens=java.base/sun.security.action=ALL-UNNAMED",
  "--add-opens=java.base/sun.util.calendar=ALL-UNNAMED"
)