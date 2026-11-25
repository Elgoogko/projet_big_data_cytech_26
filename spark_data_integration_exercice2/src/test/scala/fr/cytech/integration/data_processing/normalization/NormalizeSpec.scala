package fr.cytech.integration.data_processing.normalization

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
/**
 *
 * @see test.scala.fr.cytech.fixtures.SingleDatas
 * @see main.scala.fr.cytech.integration.Filters
 */
class NormalizeSpec extends MainSpec {

  import spark.implicits._

  // test paramétré de la fonction de normalisation des différents coûts.
  test("test fonction normalizeFees avec toutes les valeurs nulls") {
    val nullTrip: TaxiTrip = SingleDatas.validTrip.copy(
      airport_fee = null,
      congestion_surcharge = null,
      extra = null,
      mta_tax = null,
      tolls_amount = null,
      improvement_surcharge = null,
      fare_amount = null,
      cbd_congestion_fee = null)

    val df = Seq(nullTrip).toDF()
    val dfNormalized = Normalize.normalizeFees(df)

    dfNormalized.select("airport_fee").collect().head(0) shouldBe 0.0
    dfNormalized.select("congestion_surcharge").collect().head(0) shouldBe 0.0
    dfNormalized.select("extra").collect().head(0) shouldBe 0.0
    dfNormalized.select("mta_tax").collect().head(0) shouldBe 0.0
    dfNormalized.select("tolls_amount").collect().head(0) shouldBe 0.0
    dfNormalized.select("improvement_surcharge").collect().head(0) shouldBe 0.0
    dfNormalized.select("fare_amount").collect().head(0) shouldBe 0.0
    dfNormalized.select("cbd_congestion_fee").collect().head(0) shouldBe 0.0
  }

  test("test fonction normalizeFees avec certaines colonnes Nulls") {
    val nullTrip: TaxiTrip = SingleDatas.validTrip.copy(
      airport_fee = Some(0.0),
      congestion_surcharge = null,
      extra = Some(1.2),
      mta_tax = null,
      tolls_amount = Some(-10.0),
      improvement_surcharge = Some(1.1),
      fare_amount = Some(0.1),
      cbd_congestion_fee = null)

    val df = Seq(nullTrip).toDF()
    val dfNormalized = Normalize.normalizeFees(df)

    dfNormalized.select("airport_fee").collect().head(0) shouldBe 0.0
    dfNormalized.select("congestion_surcharge").collect().head(0) shouldBe 0.0
    dfNormalized.select("extra").collect().head(0) shouldBe 1.2
    dfNormalized.select("mta_tax").collect().head(0) shouldBe 0.0
    dfNormalized.select("tolls_amount").collect().head(0) shouldBe -10.0
    dfNormalized.select("improvement_surcharge").collect().head(0) shouldBe 1.1
    dfNormalized.select("fare_amount").collect().head(0) shouldBe 0.1
    dfNormalized.select("cbd_congestion_fee").collect().head(0) shouldBe 0.0
  }

  // test paramétré de la fonction normalizePaymentType
  for ((value, expected) <- NormalizeDatas.normalizePaymentTypeCases) {
    test(s"test fonction normalizePaymentType : $value -> $expected") {
      val test = Seq(SingleDatas.validTrip.copy(payment_type = value)).toDF()

      val kept = Normalize.normalizePaymentType(test).select("payment_type").collect().head(0)
      kept shouldBe expected
    }
  }

  // test paramétré de la fonction normalizeRateCode
  for ((value, expected) <- NormalizeDatas.normalizeRateCodeCases) {
    test(s"test fonction normalizeRateCode : $value -> $expected") {
      val test = Seq(SingleDatas.validTrip.copy(RatecodeID = value)).toDF()

      val kept = Normalize.normalizeRateCodeID(test).select("RatecodeID").collect().head(0)
      kept shouldBe expected
    }
  }
}