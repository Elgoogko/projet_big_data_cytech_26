package fr.cytech.fixtures

import java.time.LocalDateTime
import fr.cytech.testUtils.TaxiTrip

/**
 * Objet regroupant les différentes valeurs de tests.
 */
object ComplexDatas {

  val validTrip: TaxiTrip = TaxiTrip(
    VendorID = Some(1), tpep_pickup_datetime = Some(LocalDateTime.of(2025, 1, 1, 10, 15, 0)), tpep_dropoff_datetime = Some(LocalDateTime.of(2025, 1, 1, 10, 15, 0)),
    passenger_count = Some(1L), trip_distance = Some(2.5), RatecodeID = Some(1), store_and_fwd_flag = Some("N"),
    PULocationID = Some(10), DOLocationID = Some(20), payment_type = Some(1), fare_amount = Some(12.0), extra = Some(0.0),
    mta_tax = Some(0.5), tip_amount = Some(2.0), tolls_amount = Some(0.0), improvement_surcharge = Some(0.3),
    total_amount = Some(14.8), congestion_surcharge = Some(0.0), airport_fee = Some(0.0), cbd_congestion_fee = Some(0.0), source_month = Some("2025-01")
  )

  // cas pour la date de départ : permet de déterminé si le voyage appartient bien au mois de sont parquet (évite les doublons ou les chevauchements de parquets)
  val dateForTripCases: Seq[(Option[LocalDateTime], Option[String], Boolean)] = Seq(
    (Some(LocalDateTime.of(2026, 7, 1, 0, 0, 0)), Some("2026-07"), true),
    (Some(LocalDateTime.of(2026, 8, 1, 0, 0, 0)), Some("2026-08"), true),
    (Some(LocalDateTime.of(2026, 9, 1, 0, 0, 0)), Some("2026-09"), true),
    (Some(LocalDateTime.of(2026, 8, 1, 0, 0, 0)), Some("2026-09"), false),
    (Some(LocalDateTime.of(2025, 9, 1, 0, 0, 0)), Some("2026-09"), false),
    (Some(LocalDateTime.of(2026, 9, 1, 0, 0, 0)), Some("2020-01"), false),

  )

  val temporalCoherenceCases: Seq[(Option[LocalDateTime], Option[LocalDateTime], Boolean)] = Seq(
    (Some(LocalDateTime.of(2026, 7, 1, 0, 0, 0)), Some(LocalDateTime.of(2026, 7, 31, 23, 59, 59)), true),
    (Some(LocalDateTime.of(2026, 7, 1, 0, 0, 0)), Some(LocalDateTime.of(2026, 8, 1, 0, 0, 0)), true),
    (Some(LocalDateTime.of(2026, 7, 1, 0, 0, 0)), Some(LocalDateTime.of(2026, 6, 30, 23, 59, 59)), false),
    (Some(LocalDateTime.of(2026, 7, 1, 0, 0, 0)), None, false),
    (None, Some(LocalDateTime.of(2026, 7, 31, 23, 59, 59)), false),
    (None, None, false)
  )

  val airportFeesCases: Seq[(Option[Int], Option[Double], Boolean)] = Seq(
    (Some(100), Some(0.0), true), // pas d'aéropot + pas de surchage = valide
    (Some(132), Some(1.0), true), // aéropot + surchage = valide
    (Some(138), Some(1.0), true), // aéropot + surchage = valide
    (Some(138), Some(0.0), true), // aéropot + pas de surchage = valide
    (Some(100), Some(1.0), false), // pas d'aéropot + surchage = invalide
  )
}