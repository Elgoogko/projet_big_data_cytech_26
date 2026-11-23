package fr.cytech.integration.data_processing.normalization

import org.apache.spark.sql.{Column, DataFrame}
import org.apache.spark.sql.functions._ // col, lit, when, year, month, abs...
import java.time.LocalDateTime

/**
 * Objet dont le but est de prendre en entrée un fichier Parquet et de le normalizé (remplacer les null par des 0, par des codes par défauts ect...).
 * Le but est de rendre les filtres moins complexes par la suite.
 */
object Normalize {

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
    val nullCondition = feeColumns.map(c => col(c).isNull).reduce(_ || _)
    val affectedCount = df.filter(nullCondition).count()

    println(s"[INFO] normalizeFees : $affectedCount lignes contenaient des valeurs nulles et ont été modifiées.")
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
    //    val nullCondition = col("payment_type").isNull
    //    val affectedCount = df.filter(nullCondition).count()
    //    println(s"[INFO] normalizePaymentType : $affectedCount lignes contenaient des valeurs nulles et ont été modifiées.")

    df.na.fill(5L, Seq("payment_type"))
  }

  /**
   * Normalise la colonne RateCodeID dont le code par défaut pour les inconnus est "99"
   *
   * @param df Dataframe d'entrée
   * @return Dataframe dont RateCodeID a été normamlisé
   */
  def normalizeRateCodeID(df: DataFrame): DataFrame = {
    //    val nullCondition = col("RatecodeID").isNull
    //    val affectedCount = df.filter(nullCondition).count()
    //    println(s"[INFO] normalizeRateCodeID : $affectedCount lignes contenaient des valeurs nulles et ont été modifiées.")
    df.na.fill(99L, Seq("RatecodeID"))
  }

  def normalize(df: DataFrame): DataFrame = {
    normalizeFees(normalizePaymentType(normalizeRateCodeID(df)))
  }
}