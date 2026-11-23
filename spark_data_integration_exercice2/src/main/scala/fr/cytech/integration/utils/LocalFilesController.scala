package fr.cytech.integration.utils

import java.nio.file.{Files, StandardCopyOption, Paths, Path}
import org.apache.spark.sql.{DataFrame, SaveMode}

object LocalFilesController extends AppLogger {

  /**
   * Sauve le dataframe sous format .parquet avec un nom donnée
   *
   * @param df          un dataframe de Spark SQL
   * @param name        le nom du fichier a enregistré (sans écrire l'extension
   * @param folder_path le dossier dans lequel il sera sauvegardé
   * @return fichier enregistré ou non
   */
  def saveParquet(df: DataFrame, folder_path: String): Boolean = {
    val folder_as_Path: Path = Paths.get(folder_path)
    if (!Files.exists(folder_as_Path)) {
      logger.error(s"Le chemin spécifié n'existe pas : ${folder_path}")
      false
    } else {
      df.write
        .mode(SaveMode.Overwrite)
        .parquet(folder_path)
      true
    }
  }

  /**
   * Créer un dossier par rapport au chemin donnée
   *
   * @param folder_path chemin depuis le fichier build.sbt ou chemin absolue depuis la racine du disque
   * @return si le fichier a été créer ou non
   */
  def createFolder(folder_path: String): Boolean = {
    val folder_as_Path: Path = Paths.get(folder_path)
    if (!Files.exists(folder_as_Path)) {
      Files.createDirectories(folder_as_Path)
      logger.info(s"Dossier ${folder_path.toString} créé.")
      true
    } else {
      logger.info("Le dossier existe déjà.")
      false
    }
  }
}