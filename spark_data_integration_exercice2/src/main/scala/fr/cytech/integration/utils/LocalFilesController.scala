package fr.cytech.integration.utils

import java.nio.file.{Files, StandardCopyOption, Paths, Path}
import org.apache.spark.sql.{DataFrame, SaveMode}

object LocalFilesController extends AppLogger {

  val projectRoot: Path = Paths.get(System.getProperty("user.dir")).getParent // résout la racine du projet, grand parent de build.sbt
  val ressourcesRoot: Path = projectRoot.resolve("spark_data_integration_exercice2/src/main/resources").normalize()

  /**
   * Fonction utilitaire pour résoudre le chemin
   *
   * @param raw
   * @return
   */
  def resolvePath(raw: String): Path = {
    projectRoot.resolve(raw.stripPrefix("ROOT/")).normalize()
  }

  /**
   * Sauve le dataframe sous format .parquet avec un nom donnée
   *
   * @param df          un dataframe de Spark SQL
   * @param name        le nom du fichier a enregistré (sans écrire l'extension
   * @param folder_path le dossier dans lequel il sera sauvegardé
   * @return fichier enregistré ou non
   */
  def saveParquet(df: DataFrame, folder_path: String): Boolean = {
    val folder_as_Path: Path = resolvePath(folder_path)
    if (!Files.exists(folder_as_Path)) {
      log(LogLevel.Error, LogScope.Lifecycle)(s"Le chemin spécifié n'existe pas : ${folder_path}")
      false
    } else {
      log(LogLevel.Info, LogScope.Detail)(s"Enregistrement du dataframe dans le dossier ${folder_path}")
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
    val folder_as_Path: Path = resolvePath(folder_path)
    if (!Files.exists(folder_as_Path)) {
      Files.createDirectories(folder_as_Path)
      log(LogLevel.Info, LogScope.Detail)(s"Dossier ${folder_path.toString} créé.")
      true
    } else {
      log(LogLevel.Info, LogScope.Detail)(s"Le dossier ${folder_path.toString} existe déjà.")
      false
    }
  }
}