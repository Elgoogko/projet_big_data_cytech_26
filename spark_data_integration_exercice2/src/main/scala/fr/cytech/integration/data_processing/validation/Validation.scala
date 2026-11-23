package fr.cytech.integration.data_processing.validation

import org.apache.spark.sql.{Column, DataFrame}

import fr.cytech.integration.utils._

/*
 * Objet servant à la validation des données : 3 types de filtres.
 * Il compile aussi les données éjectés par les filtres et les combines pour les enregistré dans un dossier de quarantaine (seulement si l'option est activé)
 */
object Validation extends AppLogger {
  /**
   * Applique la pipeline de validation au dataframe donnée.
   * Crée le dataframe de quarantaine
   *
   * @param dataframe
   * @return dataframe traité et dataframe de quarantaine
   */
  def filters(dataframe: DataFrame): DataFrame = {
    log(LogLevel.Info, LogScope.Detail)(s"Filtres de validation du contrat nyc")
    val filters_step_one: DataFrame = Filters.filters(dataframe)

    log(LogLevel.Info, LogScope.Detail)(s"Filtres de validation de cohérence inter données")
    val filters_step_two: DataFrame = ComplexFilters.complexFilters(filters_step_one)

    log(LogLevel.Info, LogScope.Detail)(s"Filtres anti valeurs abérantes")
    val filters_step_three: DataFrame = AberrationFilters.aberrationFilters(filters_step_two)
    filters_step_three
  }
}