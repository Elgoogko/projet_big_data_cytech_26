package fr.cytech.dummyDatas

import java.time.LocalDateTime

/**
 * Objet regroupant les différentes valeurs de tests.
 */
object SingleDatas {

  val validTrip: TaxiTrip = TaxiTrip(
    VendorID = Some(1), tpep_pickup_datetime = Some(LocalDateTime.of(2025, 1, 1, 10, 15, 0)), tpep_dropoff_datetime = Some(LocalDateTime.of(2025, 1, 1, 10, 15, 0)),
    passenger_count = Some(1L), trip_distance = Some(2.5), RatecodeID = Some(1), store_and_fwd_flag = Some("N"),
    PULocationID = Some(10), DOLocationID = Some(20), payment_type = Some(1), fare_amount = Some(12.0), extra = Some(0.0),
    mta_tax = Some(0.5), tip_amount = Some(2.0), tolls_amount = Some(0.0), improvement_surcharge = Some(0.3),
    total_amount = Some(14.8), congestion_surcharge = Some(0.0), airport_fee = Some(0.0), cbd_congestion_fee = Some(0.0)
  )

  // Valeurs de tests pour le VendorID : seules les valeurs 1,2,6,7 fonctionnent
  val vendorCases: Seq[(Option[Int], Boolean)] = Seq(
    (Some(0), false),
    (Some(1), true),
    (Some(2), true),
    (Some(6), true),
    (Some(7), true),
    (Some(8), false),
    (Some(99), false),
    (None, false),
  )


  val pickupDropoffCases: Seq[(Option[LocalDateTime], Boolean)] = Seq(
    (Some(LocalDateTime.of(2026, 7, 15, 10, 30, 0)), true), // cas normal
    //    (Some(LocalDateTime.of(2026, 7, 1, 0, 0, 0)),    true),  // minuit pile, début de mois
    //    (Some(LocalDateTime.of(2026, 7, 31, 23, 59, 59)), true), // dernière seconde du mois
    //    (Some(LocalDateTime.of(2024, 2, 29, 12, 0, 0)),  true),  // année bissextile
    //    (Some(LocalDateTime.of(2026, 12, 31, 23, 59, 59)), true),// changement d'année
    (None, false) // null : doit être rejeté
  )

  // Valeurs de tests pour le PassengerCount : seules les valeurs "null" ou strictement positives sont acceptées
  val passengerCountCases: Seq[(Option[Long], Boolean)] = Seq(
    (Some(-10L), false),
    (Some(-1L), false),
    (Some(0L), true),
    (Some(1L), true),
    (Some(8L), true),
    (None, true),
  )

  // Valeurs de tests pour le tripDistance : seules les valeurs strictement positives sont acceptées
  val tripDistanceCases: Seq[(Option[Double], Boolean)] = Seq(
    (Some(-10.0), false),
    (Some(-1.2), false),
    (Some(-0.0001), false),
    (Some(0.0001), true),
    (Some(1.2), true),
    (Some(10.0), true),
    (None, false),
  )

  // Valeurs de tests pour le rateCode : seules les valeurs "null" et {1,2,3,4,5,6,99} sont acceptées
  val rateCodeCases: Seq[(Option[Long], Boolean)] = Seq(
    (Some(1L), true),
    (Some(99L), true),
    (Some(6L), true),
    (Some(0L), false),
    (Some(7L), false),
    (Some(98L), false),
    (None, true)
  )

  // Valeurs de tests pour le rateCode : seules les valeurs "null" et {"N", "Y"} (trimmées) sont acceptées
  val storeAndFwdFlagCases: Seq[(Option[String], Boolean)] = Seq(
    (Some("Y"), true),
    (Some("N"), true),
    (Some("YY"), false),
    (Some("NN"), false),
    (Some(" N"), true),
    (Some(" Y"), true),
    (Some("N "), true),
    (Some("Y "), true),
    (None, true),
    (Some("Test !"), false)
  )

  /**
   * Valeurs pour tester deux filtres similaires :
   * - PULoocationID
   * - DOLocationID
   *
   * Seuls les nombres dans l'intervale d'entiers positifs [1,265] sont acceptées
   */
  val locationIDCases: Seq[(Option[Int], Boolean)] = Seq(
    (None, false),
    (Some(-1), false),
    (Some(0), false),
    (Some(1), true),
    (Some(10), true),
    (Some(100), true),
    (Some(200), true),
    (Some(250), true),
    (Some(265), true),
    (Some(266), false),
    (Some(300), false)
  )

  // Valeurs de tests pour le paymentType : seules les valeurs "null" et {1,2,3,4,5,6} sont acceptées
  val paymentTypeCases: Seq[(Option[Long], Boolean)] = Seq(
    (None, true),
    (Some(0), true),
    (Some(1), true),
    (Some(2), true),
    (Some(3), true),
    (Some(4), true),
    (Some(5), true),
    (Some(6), true),
    (Some(7), false),
    (Some(200), false)
  )

  // Valeurs de tests pour le fareAmount : seules les valeurs strictement positives sont acceptées
  val fareAmountCases: Seq[(Option[Double], Boolean)] = Seq(
    (None, false),
    (Some(100.0), true),
    (Some(123465789.000), true),
    (Some(0.001), true),
    (Some(0), true),
    (Some(-1), false),
    (Some(-10.0), false),
    (Some(-1111111111111.0), false)
  )

  // Valeurs de tests pour extra : seules les valeurs "null" ou strictement positives sont acceptées
  val extraCases: Seq[(Option[Double], Boolean)] = Seq(
    (None, true),
    (Some(0.0), true),
    (Some(0.001), true),
    (Some(100.0), true),
    (Some(123465789.000), true),
    (Some(-1), false),
    (Some(-10.0), false),
    (Some(-1111111111111.0), false)
  )

  // Valeurs de tests pour le mtaTax : seules les valeurs strictement positives sont acceptées
  val mtaTaxCases: Seq[(Option[Double], Boolean)] = Seq(
    (None, false),
    (Some(0.0), true),
    (Some(0.001), true),
    (Some(100.0), true),
    (Some(123465789.000), true),
    (Some(-1), false),
    (Some(-10.0), false),
    (Some(-1111111111111.0), false)
  )

  // Valeurs de tests pour le tipAmount : seules les valeurs strictement positives sont acceptées
  val tipAmountCases: Seq[(Option[Double], Boolean)] = Seq(
    (None, true),
    (Some(0.0), true),
    (Some(0.001), true),
    (Some(100.0), true),
    (Some(123465789.000), true),
    (Some(-1), false),
    (Some(-10.0), false),
    (Some(-1111111111111.0), false)
  )

  // Valeurs de tests pour le tollsAmount : seules les valeurs "null" ou strictement positives sont acceptées
  val tollsAmountCases: Seq[(Option[Double], Boolean)] = Seq(
    (None, true),
    (Some(0.0), true),
    (Some(0.001), true),
    (Some(100.0), true),
    (Some(123465789.000), true),
    (Some(-1), false),
    (Some(-10.0), false),
    (Some(-1111111111111.0), false)
  )

  // Valeurs de tests pour le totalAmount : seules les valeurs strictement positives sont acceptées => Utile pour le Machine Learning
  val totalAmountCases: Seq[(Option[Double], Boolean)] = Seq(
    (None, false),
    (Some(0.0), true),
    (Some(0.001), true),
    (Some(100.0), true),
    (Some(123465789.000), true),
    (Some(-1), false),
    (Some(-10.0), false),
    (Some(-1111111111111.0), false)
  )

  // Valeurs de tests pour le congestionSurcharge : seules les valeurs "null" ou strictement positives sont acceptées
  val congestionSurchargeCases: Seq[(Option[Double], Boolean)] = Seq(
    (None, true),
    (Some(0.0), true),
    (Some(0.001), true),
    (Some(100.0), true),
    (Some(123465789.000), true),
    (Some(-1), false),
    (Some(-10.0), false),
    (Some(-1111111111111.0), false)
  )

  // Valeurs de tests pour le improvementSurcharge : seules les valeurs "null" ou strictement positives sont acceptées
  val improvementSurchargeCases: Seq[(Option[Double], Boolean)] = Seq(
    (None, true),
    (Some(0.0), true),
    (Some(0.001), true),
    (Some(100.0), true),
    (Some(123465789.000), true),
    (Some(-1), false),
    (Some(-10.0), false),
    (Some(-1111111111111.0), false)
  )

  // Valeurs de tests pour le airportFee : seules les valeurs "null" ou strictement positives sont acceptées
  val airportFeeCases: Seq[(Option[Double], Boolean)] = Seq(
    (None, true),
    (Some(0.0), true),
    (Some(0.001), true),
    (Some(100.0), true),
    (Some(123465789.000), true),
    (Some(-1), false),
    (Some(-10.0), false),
    (Some(-1111111111111.0), false)
  )

  // Valeurs de tests pour le cbdCongestionFee : seules les valeurs "null" ou strictement positives sont acceptées
  val cbdCongestionFeeCases: Seq[(Option[Double], Boolean)] = Seq(
    (None, true),
    (Some(0.0), true),
    (Some(0.001), true),
    (Some(100.0), true),
    (Some(123465789.000), true),
    (Some(-1), false),
    (Some(-10.0), false),
    (Some(-1111111111111.0), false)
  )
}