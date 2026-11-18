package fr.cytech.integration.validation

// Fonctions spark : spark sessions, spark SQL

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

// Sert a la conversion vers des chaines de charactères
import java.io.FileInputStream
import java.util.{Map => JMap}

/**
 * Filtres pour les valeurs abbérantes : certaines valeurs sont très hautes et sont donc a éliminé pour garder une cohérence des données
 * Les valeurs des filtres içi dépendent directement des valeurs données dans le fichier de configuration, et ne represent que sur les colonnes chiffrées.
 */
object AberrationFilters

() {

  /**
   * Fonction globale qui sert de threshold :
   * si la valeur de la colonne column est supérieur au threshold, alors elle est supprimé (considéré comme valeur abérante)
   *
   * @return
   */
  def filterByTreshold(df: DataFrame, tresholds: JMap[String, Option[Double]]): DataFrame = {
    tresholds.foldleft(df) {
      case (currentDf, (colName, maxOpt)) => maxOpt match {
        case Some(maxVal) =>
          currentDf.filter(col(colName) <= maxVal)
        case None =>
          currentDf
      }
    }
  }
}