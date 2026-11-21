package fr.cytech.integration

import org.apache.spark.sql.SparkSession

// Bibliothèques de sclatest
import org.scalatest.funsuite.AnyFunSuite // Importe "test"
import org.scalatest.matchers.should.Matchers // importe les mots clés "shouldBe", "in"....
import org.scalatest.BeforeAndAfterAll // Importe les fonctions "beforeAll", "afterAll" : permet de définir des actions à effectuer avant et après les tests


trait MainSpec extends AnyFunSuite with Matchers {
  // lazy val garantit qu'une seule instance est créée et partagée
  implicit lazy val spark: SparkSession = MainSpec.sharedSpark
}

// Companion object : garantit l'unicité de la SparkSession sur toute la suite sbt
object MainSpec {
  lazy val sharedSpark: SparkSession = {
    val session = SparkSession.builder()
      .appName("IntegrationTests")
      .master("local[*]")
      .config("spark.ui.enabled", "false")
      .getOrCreate()
    session.sparkContext.setLogLevel("ERROR")
    session
  }
}