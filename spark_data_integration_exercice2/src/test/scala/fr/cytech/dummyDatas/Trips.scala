package fr.cytech.dummyDatas

import java.time.LocalDateTime

object Trips {

  val t1 = LocalDateTime.of(2025, 1, 1, 10, 15, 0)
  val t2 = LocalDateTime.of(2025, 1, 1, 10, 45, 0)

  def base: TaxiTrip = TaxiTrip(
    VendorID = Some(1), tpep_pickup_datetime = Some(t1), tpep_dropoff_datetime = Some(t2),
    passenger_count = Some(1L), trip_distance = Some(2.5), RatecodeID = Some(1), store_and_fwd_flag = Some("N"),
    PULocationID = Some(10), DOLocationID = Some(20), payment_type = Some(1), fare_amount = Some(12.0), extra = Some(0.0),
    mta_tax = Some(0.5), tip_amount = Some(2.0), tolls_amount = Some(0.0), improvement_surcharge = Some(0.3),
    total_amount = Some(14.8), congestion_surcharge = Some(0.0), airport_fee = Some(0.0), cbd_congestion_fee = Some(0.0)
  )

  // (trip, kept) — kept = what cleanDF should return
  val trips: Seq[(String, TaxiTrip, Boolean)] = Seq(
    ("valid baseline", base, true),

    // 1. VendorID not in (1,2,6,7)
    ("invalid VendorID", base.copy(VendorID = Some(5)), false),

    // 2. null pickup datetime
    ("null pickup datetime", base.copy(tpep_pickup_datetime = None), false),

    // 3. negative passenger count
    ("negative passenger_count", base.copy(passenger_count = Some(-1L)), false),

    // 4. null trip_distance
    ("null trip_distance", base.copy(trip_distance = None), false),

    // 5. RatecodeID out of range
    ("invalid RatecodeID", base.copy(RatecodeID = Some(7)), false),

    // 6. malformed store_and_fwd_flag (trimmed but not N/Y)
    ("invalid store_and_fwd_flag", base.copy(store_and_fwd_flag = Some(" X ")), false),

    // 7. PULocationID out of range (266 = JFK boundary excluded)
    ("invalid PULocationID", base.copy(PULocationID = Some(266)), false),

    // 8. payment_type out of range
    ("invalid payment_type", base.copy(payment_type = Some(7)), false),

    // 9. negative total_amount
    ("negative total_amount", base.copy(total_amount = Some(-5.0)), false),

    // 10. null mta_tax -> rejected because validMtaTax has no isNull clause
    ("null mta_tax", base.copy(mta_tax = None), false),
  )
}