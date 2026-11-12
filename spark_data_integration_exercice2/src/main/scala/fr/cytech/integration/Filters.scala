package fr.cytech.integration

import org.apache.spark.sql.{Column, DataFrame}
import org.apache.spark.sql.functions._ // col, lit, when, year, month, abs...
import java.time.LocalDateTime

/**
 * Liste de premiers tests de validations sur un fichier parquet.
 *
 * Chaque test est basé sur le "dictionnaire des colonnes" donnée par le fournisseurs des parquet, NYC Taxis.
 * Ces tests sont, à la fin, combiné entre eux pour un premier nettoyage de dataframe.
 */
object Filters {
  def validVendorID: Column = col("VendorID").isin(1, 2, 6, 7) && col("VendorID").isNotNull

  def validTpepPickupDatetime: Column = col("tpep_pickup_datetime").isNotNull

  def validTpepDropoffDatetime: Column = col("tpep_dropoff_datetime").isNotNull

  def validPassengerCount: Column = col("passenger_count").isNull || col("passenger_count") >= 0

  def validTripDistance: Column = col("trip_distance").isNotNull && col("trip_distance") >= 0

  def validRateCodeID: Column = col("RatecodeID").isNull || col("RatecodeID").isin(1, 2, 3, 4, 5, 6, 99)

  def validStoreAndFwdFlag: Column = col("store_and_fwd_flag").isNull || trim(col("store_and_fwd_flag")).isin("N", "Y")

  def validPULocationID: Column = col("PULocationID").isNotNull && col("PULocationID") > 0 && col("PULocationID") < 266

  def validDOLocationID: Column = col("DOLocationID").isNotNull && col("DOLocationID") > 0 && col("DOLocationID") < 266

  def validPaymentType: Column = col("payment_type").isNull || col("payment_type").isin(0, 1, 2, 3, 4, 5, 6)

  def validFareAmount: Column = col("fare_amount").isNotNull && col("fare_amount") >= 0

  def validExtra: Column = col("extra").isNull || col("extra") >= 0

  def validMtaTax: Column = col("mta_tax") >= 0

  def validTipAmount: Column = col("tip_amount").isNull || col("tip_amount") >= 0

  def validTollsAmount: Column = col("tolls_amount").isNull || col("tolls_amount") >= 0

  def validTotalAmount: Column = col("total_amount").isNotNull && col("total_amount") >= 0

  def validCongestionSurcharge: Column = col("congestion_surcharge").isNull || col("congestion_surcharge") >= 0

  def validImprovementSurcharge: Column = col("improvement_surcharge").isNull || col("improvement_surcharge") >= 0

  def validAirportFee: Column = col("airport_fee").isNull || col("airport_fee") >= 0

  def validCbdCongestionFee: Column = col("cbd_congestion_fee").isNull || col("cbd_congestion_fee") >= 0

  // Fonction de nettoyage du dataframe final, combinant tous les tests précédents
  val all: Seq[Column] = Seq(validVendorID, validTpepPickupDatetime, validTpepDropoffDatetime, validPassengerCount, validTripDistance, validRateCodeID, validStoreAndFwdFlag, validPULocationID, validDOLocationID, validPaymentType, validFareAmount, validExtra, validMtaTax, validTipAmount, validTollsAmount, validImprovementSurcharge, validTotalAmount, validCongestionSurcharge, validAirportFee, validCbdCongestionFee)

  def cleanDF(df: DataFrame): DataFrame = df.filter(all.reduce(_ && _))
}