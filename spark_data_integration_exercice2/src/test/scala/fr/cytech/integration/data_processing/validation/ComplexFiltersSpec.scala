package fr.cytech.integration.data_processing.validation

import org.apache.spark.sql.SparkSession

// Bibliothèques de sclatest
import org.scalatest.funsuite.AnyFunSuite // Importe "test"
import org.scalatest.matchers.should.Matchers // importe les mots clés "shouldBe", "in"....

// Bibliothèques de spark SQL : permet de manipulé des objets scala comme des tableau SQL
import org.apache.spark.sql.{Column, DataFrame}
import org.apache.spark.sql.functions._ // col, lit, when, year, month, abs...

// Données de tests
import fr.cytech.fixtures._
import fr.cytech.testUtils.TaxiTrip
import fr.cytech.integration.MainSpec

/**
 * Cette class teste les premiers filtres de validation des données un par un se trouvant dans "Filters".
 * Chaque filtre est testé avec N posssibilités définis dans les données de tests.
 *
 * @see test.scala.fr.cytech.fixtures.SingleDatas
 * @see main.scala.fr.cytech.integration.Filters
 */
class ComplexFiltersSpec extends MainSpec {

  import spark.implicits._

  for ((value, expected_start, expected_end) <- ComplexDatas.monthBoundsCases) {
    test(s"MonthBounds Test : $value -> $expected_start, $expected_end") {
      val (start, end) = ComplexFilters.monthBounds(value)
      start shouldBe expected_start
      end shouldBe expected_end
    }
  }

  test("Exception Levé sur MonthBounds : Nom du fichier non conforme") {
    val fileName = "2026-08.parquet"
    val exception = intercept[IllegalArgumentException] {
      ComplexFilters.monthBounds(fileName)
    }
    exception.getMessage() shouldBe "Nom du fichier invalide, format attendu : 'yellow_tripdata_YYYY-MM.parquet'. Valeur reçue : 2026-08.parquet"
  }

  test("Exception Levé sur MonthBounds : chaine de caractères vide") {
    val fileName = ""
    val exception = intercept[IllegalArgumentException] {
      ComplexFilters.monthBounds(fileName)
    }
    exception.getMessage() shouldBe "Nom du fichier invalide, format attendu : 'yellow_tripdata_YYYY-MM.parquet'. Valeur reçue : "
  }

  // test sur différentes dates (est ce que le voyage appartient bien au mois)
  for ((value, expected) <- ComplexDatas.pickupCases) {
    test(s"test pickupcases : $value -> $expected") {

      val fileName = "yellow_tripdata_2026-07.parquet"

      val (start, end) = ComplexFilters.monthBounds(fileName)
      val kept = Seq(SingleDatas.validTrip.copy(tpep_pickup_datetime = value)).toDF().filter(ComplexFilters.validDateForTrip(start, end)).count() == 1
      kept shouldBe expected
    }
  }
}