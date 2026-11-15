package fr.cytech.integration.validation

import org.apache.spark.sql.{Column, DataFrame}
import org.apache.spark.sql.functions._ // col, lit, when, year, month, abs...
import java.time.LocalDateTime
import java.sql.Timestamp
import java.time.YearMonth

/**
 * Cette classe de filtres vérifie la cohérence des données : dates, taxes valides ou non
 */
object ComplexFilters {

  /**
   * Helper pour avoir les bornes pour la fonction "validDateForTrip"
   *
   * @param fileName Nom du parquet à validé, le mois se trouve dans le nom
   * @return La borne "EndDate" du parquet
   */
  def monthBounds(fileName: String): (LocalDateTime, LocalDateTime) = {
    val ym = YearMonth.parse(fileName.replaceAll(".*_(\\d{4}-\\d{2})\\.parquet", "$1"))
    (ym.atDay(1).atStartOfDay(), ym.plusMonths(1).atDay(1).atStartOfDay())
  }

  /**
   * Vérifie que les voyages sont strictement rataché au mois en cours.
   * Par convention:  un voyage commençant le mois précédent et se terminant le mois en cours est rataché au mois précédent.
   * Au contraire : un voyage commmençant dans le mois actuel et finissant dans le mois suivant est rataché au mois actuel.
   *
   * @param startDate 1er du mois actuel (en incluant année et mois)
   * @param endDate   Le 1er du mois suivant à 00h00 exactement.
   * @return Les colonnes qui respectent ce filtrage
   */
  def validDateForTrip(startDate: LocalDateTime, endDate: LocalDateTime): Column = {
    col("tpep_pickup_datetime") >= lit(Timestamp.valueOf(startDate)) &&
      col("tpep_pickup_datetime") < lit(endDate)
  }

  /**
   * Vérifie la cohérence temporel: l'arrivé se fait strictement après le départ
   *
   * @return Les colonnes qui respectent ce filtrage
   */
  def validTemporalCoherence(): Column = {
    col("tpep_dropoff_datetime") > col("tpep_pickup_datetime")
  }

  /**
   * Vérifie la cohérence des taxes : le airportFee ne peux pas être appliqué si la départ n'est pas un aéroport.
   * Seulement pour les PULocationCode = 138; 132
   *
   * @return
   */
  def validAirportFees: Column = !(col("airport_fee") > 0) || col("PULocationID").isin(132, 138)
}