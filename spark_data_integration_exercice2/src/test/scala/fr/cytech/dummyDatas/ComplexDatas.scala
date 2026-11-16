package fr.cytech.dummyDatas

import java.time.LocalDateTime

/**
 * Objet regroupant les différentes valeurs de tests.
 */
object ComplexDatas {

  val validTrip: TaxiTrip = TaxiTrip(
    VendorID = Some(1), tpep_pickup_datetime = Some(LocalDateTime.of(2025, 1, 1, 10, 15, 0)), tpep_dropoff_datetime = Some(LocalDateTime.of(2025, 1, 1, 10, 15, 0)),
    passenger_count = Some(1L), trip_distance = Some(2.5), RatecodeID = Some(1), store_and_fwd_flag = Some("N"),
    PULocationID = Some(10), DOLocationID = Some(20), payment_type = Some(1), fare_amount = Some(12.0), extra = Some(0.0),
    mta_tax = Some(0.5), tip_amount = Some(2.0), tolls_amount = Some(0.0), improvement_surcharge = Some(0.3),
    total_amount = Some(14.8), congestion_surcharge = Some(0.0), airport_fee = Some(0.0), cbd_congestion_fee = Some(0.0)
  )

  val monthBoundsCases: Seq[(String, LocalDateTime, LocalDateTime)] = Seq(
    ("yellow_tripdata_2026-07.parquet", LocalDateTime.of(2026, 7, 1, 0, 0, 0), LocalDateTime.of(2026, 8, 1, 0, 0, 0)),
    ("yellow_tripdata_2026-08.parquet", LocalDateTime.of(2026, 8, 1, 0, 0, 0), LocalDateTime.of(2026, 9, 1, 0, 0, 0)),
    ("yellow_tripdata_2026-09.parquet", LocalDateTime.of(2026, 9, 1, 0, 0, 0), LocalDateTime.of(2026, 10, 1, 0, 0, 0))
  )

  val pickupCases: Seq[(Option[LocalDateTime], Boolean)] = Seq(
    (Some(LocalDateTime.of(2026, 7, 15, 10, 30, 0)), true), // cas normal
    (Some(LocalDateTime.of(2026, 7, 1, 0, 0, 0)), true), // minuit pile, début de mois
    (Some(LocalDateTime.of(2026, 7, 31, 23, 59, 59)), true), // dernière seconde du mois
    (Some(LocalDateTime.of(2024, 2, 29, 12, 0, 0)), false), // année bissextile
    (Some(LocalDateTime.of(2026, 12, 31, 23, 59, 59)), false), // changement d'année
    (None, false) // null : doit être rejeté
  )
}