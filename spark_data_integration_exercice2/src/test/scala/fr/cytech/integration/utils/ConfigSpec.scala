package fr.cytech.integration.utils

// Bibliothèques de sclatest

import org.scalatest.funsuite.AnyFunSuite // Importe "test"
import org.scalatest.matchers.should.Matchers // importe les mots clés "shouldBe", "in"....

// Bibliothèques de spark SQL : permet de manipulé des objets scala comme des tableau SQL
import org.apache.spark.sql.{Column, DataFrame}
import org.apache.spark.sql.functions._ // col, lit, when, year, month, abs...
import java.time.LocalDateTime

// Données de tests
import fr.cytech.fixtures._
import fr.cytech.testUtils.TaxiTrip
import fr.cytech.integration.MainSpec
import fr.cytech.fixtures.configs_yaml._

class ConfigSpec extends MainSpec {

  import spark.implicits._

  Config.init("src/test/scala/fr/cytech/fixtures/configs_yaml/default_config.yaml")

  test("Config validation : vérifie la validité des fichiers de configuration") {
    Config.get_rejected shouldBe false
  }

  def validate_config_fields(name: String, config: Map[String, Any]): Unit = {
    test(f"vérification des clés pour $name") {
      for ((key, expected) <- config) {
        val result = Config.getSetting(key)
        result shouldBe expected
      }
    }
  }
}