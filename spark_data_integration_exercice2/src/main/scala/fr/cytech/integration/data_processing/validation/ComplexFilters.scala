package fr.cytech.integration.data_processing.validation

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
   * Vérifie que les voyages sont strictement rataché au mois en cours.
   * Par convention:  un voyage commençant le mois précédent et se terminant le mois en cours est rataché au mois précédent.
   * Au contraire : un voyage commmençant dans le mois actuel et finissant dans le mois suivant est rataché au mois actuel.
   *
   * @return Les colonnes qui respectent ce filtrage
   */
  def validDateForTrip: Column = date_format(col("tpep_pickup_datetime"), "yyyy-MM") === col("source_month")

  /**
   * Vérifie la cohérence temporel: l'arrivé se fait strictement après le départ
   *
   * @return Les colonnes qui respectent ce filtrage
   */
  def validTemporalCoherence: Column = {
    col("tpep_dropoff_datetime") > col("tpep_pickup_datetime")
  }

  /**
   * Vérifie la cohérence des taxes : le airportFee ne peux pas être appliqué si la départ n'est pas un aéroport.
   * Seulement pour les PULocationCode = 138; 132
   *
   * Attention : ça ne filtre pas le contraire, partir de l'aéroport mais avec une surchage nul n'élimine pas la ligne.
   *
   * @return les colonnes qui respecetent ce filtrage
   */
  def validAirportFees: Column = !(col("airport_fee") > 0) || col("PULocationID").isin(132, 138)

  def complexFilters(dataframe: DataFrame): DataFrame = {
    dataframe
      .filter(validDateForTrip)
      .filter(validTemporalCoherence)
      .filter(validAirportFees)
  }
}