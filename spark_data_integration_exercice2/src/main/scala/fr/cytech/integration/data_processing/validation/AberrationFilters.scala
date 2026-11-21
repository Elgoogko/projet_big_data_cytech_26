package fr.cytech.integration.data_processing.validation

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.col
import scala.jdk.CollectionConverters._
import java.util.{Map => JMap}

/**
 * Filtres pour les valeurs aberrantes : certaines valeurs sont très hautes et sont donc à éliminer
 * pour garder une cohérence des données.
 * Les valeurs des filtres dépendent directement du fichier de configuration et ne s'appliquent
 * que sur les colonnes chiffrées.
 */
object AberrationFilters {

  /**
   * Fonction globale de threshold :
   * si la valeur d'une colonne est supérieure au threshold, la ligne est supprimée (considérée comme aberrante).
   *
   * @param df         DataFrame Spark à filtrer
   * @param thresholds Map Java associant le nom de la colonne à son seuil maximal optionnel
   * @return DataFrame filtré
   */
  def filterByThreshold(df: DataFrame, thresholds: JMap[String, Option[Double]]): DataFrame = {
    thresholds.asScala.foldLeft(df) {
      case (currentDf, (colName, Some(maxVal))) =>
        currentDf.filter(col(colName) <= maxVal)
      case (currentDf, (_, None)) =>
        currentDf
    }
  }
}