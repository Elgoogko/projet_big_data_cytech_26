package fr.cytech.integration

import org.apache.spark.sql.SparkSession

// Bibliothèques de sclatest
import org.scalatest.funsuite.AnyFunSuite // Importe "test"
import org.scalatest.matchers.should.Matchers // importe les mots clés "shouldBe", "in"....
import org.scalatest.BeforeAndAfterAll // Importe les fonctions "beforeAll", "afterAll" : permet de définir des actions à effectuer avant et après les tests

// Bibliothèques de spark SQL : permet de manipulé des objets scala comme des tableau SQL
import org.apache.spark.sql.{Column, DataFrame}
import org.apache.spark.sql.functions._ // col, lit, when, year, month, abs...

// Données de tests
import fr.cytech.dummyDatas._

/**
 * Cette class teste les premiers filtres de validation des données un par un se trouvant dans "Filters".
 * Chaque filtre est testé avec N posssibilités définis dans les données de tests.
 *
 * @see test.scala.fr.cytech.dummyDatas.SingleDatas
 * @see main.scala.fr.cytech.integration.Filters
 */
class FiltersTest extends AnyFunSuite with Matchers with BeforeAndAfterAll {

  // Création de la session spark
  implicit val spark: SparkSession = SparkSession.builder()
    .appName("FilterTest")
    .master("local[*]")
    .getOrCreate()

  import spark.implicits._

  spark.sparkContext.setLogLevel("ERROR") // Réduit les logs inutiles

  // Fermeture de la session spark après les tests
  override def afterAll(): Unit = {
    spark.stop()
    super.afterAll()
  }

  /**
   * Fonction de test de filtres.
   * Prend en entrée un filtre, des cas et les fais passer dans un test simple.
   *
   * @param name   Nom du filtre à tester, pour l'affichage terminal
   * @param filter Fonction filtre à tester
   * @param cases  Cas de tests sous la forme (Valeur, résultat attendu)
   * @param build  Fonction de construction des cas de test (en modifiant un cas valide)
   * @tparam T Type lambda
   */
  def checkFilter[T](name: String, filter: Column, cases: Seq[(T, Boolean)])
                    (build: T => TaxiTrip): Unit = {
    for ((value, expected) <- cases) {
      test(s"$name : $value -> $expected") {
        val kept = Seq(build(value)).toDF().filter(filter).count() == 1
        kept shouldBe expected
      }
    }
  }

  // Série de tests utilisant la fonction checkFilter sur les différentes colonnes pour tester les filtres un par un.
  checkFilter("validVendorID", Filters.validVendorID, SingleDatas.vendorCases)(v => SingleDatas.validTrip.copy(VendorID = v))

  checkFilter("validTpepPickupDatetime", Filters.validTpepPickupDatetime, SingleDatas.pickupDropoffCases)(v => SingleDatas.validTrip.copy(tpep_pickup_datetime = v))

  checkFilter("validTpepDropoffDatetime", Filters.validTpepDropoffDatetime, SingleDatas.pickupDropoffCases)(v => SingleDatas.validTrip.copy(tpep_dropoff_datetime = v))

  checkFilter("validPassengerCount", Filters.validPassengerCount, SingleDatas.passengerCountCases)(v => SingleDatas.validTrip.copy(passenger_count = v))

  checkFilter("validTripDistance", Filters.validTripDistance, SingleDatas.tripDistanceCases)(v => SingleDatas.validTrip.copy(trip_distance = v))

  checkFilter("validRateCodeID", Filters.validRateCodeID, SingleDatas.rateCodeCases)(v => SingleDatas.validTrip.copy(RatecodeID = v))

  checkFilter("validStoreAndFwdFlag", Filters.validStoreAndFwdFlag, SingleDatas.storeAndFwdFlagCases)(v => SingleDatas.validTrip.copy(store_and_fwd_flag = v))

  checkFilter("validPULocationID", Filters.validPULocationID, SingleDatas.locationIDCases)(v => SingleDatas.validTrip.copy(PULocationID = v))

  checkFilter("validDOLocationID", Filters.validDOLocationID, SingleDatas.locationIDCases)(v => SingleDatas.validTrip.copy(DOLocationID = v))

  checkFilter("validPaymentType", Filters.validPaymentType, SingleDatas.paymentTypeCases)(v => SingleDatas.validTrip.copy(payment_type = v))

  checkFilter("validFareAmount", Filters.validFareAmount, SingleDatas.fareAmountCases)(v => SingleDatas.validTrip.copy(fare_amount = v))

  checkFilter("validExtra", Filters.validExtra, SingleDatas.extraCases)(v => SingleDatas.validTrip.copy(extra = v))

  checkFilter("validMtaTax", Filters.validMtaTax, SingleDatas.mtaTaxCases)(v => SingleDatas.validTrip.copy(mta_tax = v))

  checkFilter("validTipAmount", Filters.validTipAmount, SingleDatas.tipAmountCases)(v => SingleDatas.validTrip.copy(tip_amount = v))

  checkFilter("validTollsAmount", Filters.validTollsAmount, SingleDatas.tollsAmountCases)(v => SingleDatas.validTrip.copy(tolls_amount = v))

  checkFilter("validTotalAmount", Filters.validTotalAmount, SingleDatas.totalAmountCases)(v => SingleDatas.validTrip.copy(total_amount = v))

  checkFilter("validCongestionSurcharge", Filters.validCongestionSurcharge, SingleDatas.congestionSurchargeCases)(v => SingleDatas.validTrip.copy(congestion_surcharge = v))

  checkFilter("validImprovementSurcharge", Filters.validImprovementSurcharge, SingleDatas.improvementSurchargeCases)(v => SingleDatas.validTrip.copy(improvement_surcharge = v))

  checkFilter("validAirportFee", Filters.validAirportFee, SingleDatas.airportFeeCases)(v => SingleDatas.validTrip.copy(airport_fee = v))

  checkFilter("validCbdCongestionFee", Filters.validCbdCongestionFee, SingleDatas.cbdCongestionFeeCases)(v => SingleDatas.validTrip.copy(cbd_congestion_fee = v))
}