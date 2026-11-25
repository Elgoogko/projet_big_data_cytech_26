package fr.cytech.integration.data_processing.normalization

import org.apache.spark.sql.{Column, DataFrame}
import org.apache.spark.sql.functions._ // col, lit, when, year, month, abs...
import java.time.LocalDateTime

import fr.cytech.integration.utils._

/**
 * Objet dont le but est de prendre en entrée un fichier Parquet et de le normalizé (remplacer les null par des 0, par des codes par défauts ect...).
 * Le but est de rendre les filtres moins complexes par la suite.
 */
object Normalize extends AppLogger {

  val feeColumns = Seq(
    "airport_fee", "congestion_surcharge", "extra", "mta_tax",
    "tolls_amount", "improvement_surcharge", "fare_amount", "cbd_congestion_fee"
  )

  /**
   * Remplace les "null" potentiels des colonnes de prix par des 0
   *
   * @param df Dataframe d'entrée
   * @return Un dataframe normalisé
   */
  def normalizeFees(df: DataFrame): DataFrame = {
    if (LogConfig.getVerbosity == Verbosity.F) {
      val nullCondition = feeColumns.map(c => col(c).isNull).reduce(_ || _)
      val affectedCount = df.filter(nullCondition).count()

      log(LogLevel.Info, LogScope.Detail)(s"normalizeFees : $affectedCount lignes contenaient des valeurs nulles et ont été modifiées.")
    }
    df.na.fill(0.0, feeColumns)
  }

  /**
   * Sur le dataframe : la colonne payment_type peut contenir des valeurs "null" ce qui signifie qu'on en connait pas le mode de payment (erreur d'enregistrement).
   * Or, il existe déjà un code pour ça : le 5 (Unknown).
   * Cette fonction remplace tous les "null" de la colonne par le code "inconnu" 5
   *
   * @param df Dataframe d'entrée
   * @return Dataframe dont payment_type a été normamlisé
   */
  def normalizePaymentType(df: DataFrame): DataFrame = {
    logDetail("payment_type", df)
    df.na.fill(5L, Seq("payment_type"))
  }

  /**
   * Normalise la colonne RateCodeID dont le code par défaut pour les inconnus est "99"
   *
   * @param df Dataframe d'entrée
   * @return Dataframe dont RateCodeID a été normamlisé
   */
  def normalizeRateCodeID(df: DataFrame): DataFrame = {
    logDetail("RatecodeID", df)
    df.na.fill(99L, Seq("RatecodeID"))
  }

  /**
   * Fonction global de logs des détails des données
   *
   * @param col_name
   */
  def logDetail(col_name: String, df: DataFrame): Unit = {
    if (LogConfig.getVerbosity == Verbosity.F) {
      val nullCondition = col(col_name).isNull
      val affectedCount = df.filter(nullCondition).count()
      log(LogLevel.Info, LogScope.Detail)(s"${col_name} : $affectedCount lignes contenaient des valeurs nulles et ont été modifiées.")
    }
  }

  def normalize(df: DataFrame): DataFrame = {
    normalizeFees(normalizePaymentType(normalizeRateCodeID(df)))
  }
}