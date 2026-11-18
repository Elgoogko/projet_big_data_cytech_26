package fr.cytech.fixtures

import java.time.LocalDateTime
import fr.cytech.testUtils.TaxiTrip

/**
 * Objet regroupant les différentes valeurs de tests.
 */
object NormalizeDatas {
  val validTrip: TaxiTrip = TaxiTrip(
    VendorID = Some(1), tpep_pickup_datetime = Some(LocalDateTime.of(2025, 1, 1, 10, 15, 0)), tpep_dropoff_datetime = Some(LocalDateTime.of(2025, 1, 1, 10, 15, 0)),
    passenger_count = Some(1L), trip_distance = Some(2.5), RatecodeID = Some(1), store_and_fwd_flag = Some("N"),
    PULocationID = Some(10), DOLocationID = Some(20), payment_type = Some(1), fare_amount = Some(12.0), extra = Some(0.0),
    mta_tax = Some(0.5), tip_amount = Some(2.0), tolls_amount = Some(0.0), improvement_surcharge = Some(0.3),
    total_amount = Some(14.8), congestion_surcharge = Some(0.0), airport_fee = Some(0.0), cbd_congestion_fee = Some(0.0)
  )

  // Valeur de test pour payment type : toutes les valeurs reste, les null sont transformé en 5
  val normalizePaymentTypeCases: Seq[(Option[Long], Long)] = Seq(
    (None, 5L),
    (Some(0L), 0L),
    (Some(1L), 1L),
    (Some(2L), 2L),
    (Some(3L), 3L),
    (Some(4L), 4L),
    (Some(5L), 5L),
    (Some(6L), 6L),
  )

  // Valeur de test pour RateCode : toutes les valeurs reste, les null sont transformé en 5
  val normalizeRateCodeCases: Seq[(Option[Long], Long)] = Seq(
    (None, 99L),
    (Some(1L), 1L),
    (Some(2L), 2L),
    (Some(3L), 3L),
    (Some(4L), 4L),
    (Some(5L), 5L),
    (Some(6L), 6L),
  )
}