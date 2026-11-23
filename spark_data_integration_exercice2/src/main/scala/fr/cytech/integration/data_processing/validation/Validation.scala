package fr.cytech.integration.data_processing.validation

import org.apache.spark.sql.{Column, DataFrame}

/**
 * Objet servant à la validation des données : 3 types de filtres.
 * Il compile aussi les données éjectés par les filtres et les combines pour les enregistré dans un dossier de quarantaine (seulement si l'option est activé)
 */
object Validation {

  /**
   * Applique la pipeline de validation à ce fichier
   *
   * @param dataframe
   */
  def filters(dataframe: DataFrame): DataFrame = {
    val filters_step_one: DataFrame = Filters.filters(dataframe)
    val filters_step_two: DataFrame = ComplexFilters.complexFilters(filters_step_one)
    val filters_step_three: DataFrame = AberrationFilters.aberrationFilters(filters_step_two)
    filters_step_three
  }
}