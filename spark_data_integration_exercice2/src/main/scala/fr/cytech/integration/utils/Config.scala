package fr.cytech.integration.utils

import org.yaml.snakeyaml.Yaml

import scala.io.Source
import scala.jdk.CollectionConverters._
import scala.util.{Try, Using}
import java.nio.file.{Files, StandardCopyOption, Paths, Path}

import java.util.{List => JList, Map => JMap}

/**
 * Objet de configuration de l'application.
 * gère les paramètres globaux comme les thresholds, les liens et les options de runtime (verbose, quarantine)
 */
object Config extends AppLogger {

  /**
   * Conversion dynamique pour être sur de convertir les strings en double, les string en int, ou le contraire
   *
   * @param value paramètre inconnu a évaluer
   */
  case class SettingValue(value: Any) {
    def asString: String = value.toString

    def asDouble: Double = value match {
      case n: Number => n.doubleValue()
      case s: String => s.toDouble
      case other => throw new ClassCastException(s"Impossible de convertir $other en Double")
    }

    def asInt: Int = value match {
      case n: Number => n.intValue()
      case s: String => s.toInt
      case other => throw new ClassCastException(s"Impossible de convertir $other en Int")
    }

    def asBoolean: Boolean = value match {
      case b: Boolean => b
      case s: String => s.toBoolean
      case other => throw new ClassCastException(s"Impossible de convertir $other en Boolean")
    }

    def asStringList: List[String] = value match {
      case l: List[_] => l.map(_.toString)
      case other => throw new ClassCastException(s"Impossible de convertir $other en List[String]")
    }
    def asOptDouble: Option[Double] = Option(value).map(_ => asDouble) // sert seulement pour les seuils qui renvoie soit un nombre soit null
  }


  // Schéma de référence pour la validation de la configuration et configuration par défaut si config invalide
  private val defaultConfig: Map[String, Any] = Map(
    "filters_thresholds" -> Map(
      "fare_amount" -> null,
      "extra" -> null,
      "mta_tax" -> null,
      "tip_amount" -> null,
      "tolls_amount" -> null,
      "improvement_surcharge" -> null,
      "total_amount" -> null,
      "congestion_surcharge" -> null,
      "Airport_fee" -> null,
      "cbd_congestion_fee" -> null
    ),
    "app" -> Map(
      "spark" -> Map(
        "app_name" -> "spark_data_integration_exercice2",
        "master" -> "local[*]",
        "local_copy" -> true
      ),
      "s3" -> Map(
        "endpoint" -> "http://localhost:9000",
        "nyc_bucket" -> "s3a://nyc-bucket/",
        "raw_path" -> "nyc_raw",
        "processed_path" -> "nyc_processed",
        "parquets" -> List("yellow_tripdata_2026-07.parquet")
      ),
      "data_processing" -> Map(
        "quarantine" -> true,
        "quarantine_path" -> "nyc_quarantine"
      ),
  "local_paths" -> Map(
    "raw_local_path" -> "data/raw",
    "processed_local_path" -> "data/processed",
    "quarantine_local_path" -> "data/quarantine"
  )
    )
  )

  // Chemin vers le fichier de configuration, par défaut redirigé vers le fichier default_config.yaml
  private var configPath: Path = LocalFilesController.ressourcesRoot.resolve("default_config.yaml")
  private var rejected: Boolean = false

  /**
   * Renvoi la variable rejected :
   * - False : le fichier donnée au départ à été validé et est en cours d'utilisation
   * - True : le fichier donnée au départ est invalide, la configurayion par defaut est utilisé
   *
   * @return
   */
  def get_rejected: Boolean = rejected

  /**
   * Initilisation de la classe de configuration
   *
   * @param path chemin vers le fichier de configuration
   */
  def init(path: String): Unit = {
    configPath = LocalFilesController.ressourcesRoot.resolve(path)
  }

  // Chargement paresseux et unique de l'arbre complet
  lazy val settings: Map[String, Any] = loadConfig(configPath)

  // Index aplati (clé -> valeur) généré une seule fois pour un accès direct par nom
  private lazy val flatSettings: Map[String, Any] = flatten(settings)

  /**
   * Appel d'un paramètre en particulier
   *
   * @param key clé du dictionnaire chercher
   * @throws NoSuchElementException Si la clé demandé n'existe pas sur le dictionnaire
   * @return la valeur associé à la clé
   */
  def getSetting(key: String): SettingValue = {
    flatSettings.get(key) match {
      case Some(v) => SettingValue(v)
      case None => throw new NoSuchElementException(s"Clé de configuration introuvable : '$key'")
    }
  }

  /**
   * Chargement et validation du fichier de configuration
   *
   * @param path
   * @return
   */
  private def loadConfig(path: Path): Map[String, Any] = {
    val parsed = Using(Source.fromFile(path.toFile)) { source =>
      val yaml = new Yaml()
      val raw = yaml.load[Any](source.mkString)
      toScala(raw) match {
        case m: Map[_, _] => m.asInstanceOf[Map[String, Any]]
        case _ => Map.empty[String, Any]
      }
    }

    parsed.toOption.map(cfg => cfg -> schemaErrors(defaultConfig, cfg)) match {
      case Some((cfg, Nil)) => cfg
      case Some((_, errors)) => fallbackToDefault(s"$path : ${errors.mkString("\n  - ", "\n  - ", "")}")
      case None => fallbackToDefault(s"$path introuvable ou illisible")
    }
  }

  /**
   * Convertit récursivement les Maps et Listes Java de SnakeYAML en collections Scala
   *
   * @param obj objet a convertir en scala
   * @return objet convertit
   */
  private def toScala(obj: Any): Any = obj match {
    case m: JMap[_, _] =>
      m.asScala.map { case (k, v) => k.toString -> toScala(v) }.toMap
    case l: JList[_] =>
      l.asScala.map(toScala).toList
    case other =>
      other
  }

  /**
   * Vérifie qu'une valeur lue dans le YAML a le même type que la valeur de référence.
   * Un null est accepté pour un Double : il signifie "seuil désactivé".
   *
   * @param expected valeur de référence issue de defaultConfig
   * @param actual   valeur lue dans le fichier YAML (peut être null)
   * @return true si les types sont compatibles
   */
  private def typeMatches(expected: Any, actual: Any): Boolean = (expected, actual) match {
    case (null, null) => true // seuil désactivé dans le défaut et dans le YAML
    case (null, _: Number) => true // défaut désactivé, YAML avec un seuil
    case (_: Number, null) => true // défaut avec un seuil, YAML désactivé
    case (_: Number, _: Number) => true // Int et Double interchangeables (YAML lit "0" en Int)
    case (_: Double, null) => true
    case (_: Double, _: Number) => true
    case (_: Int, _: Int) => true
    case (_: Boolean, _: Boolean) => true
    case (_: String, _: String) => true
    case (_: List[_], _: List[_]) => true
    case _ => false
  }

  /**
   * Compare une seule valeur à son modèle. Descend récursivement si c'est une sous-section.
   *
   * @param path     chemin complet de la clé (ex : "app.s3.endpoint"), sert aux messages d'erreur
   * @param expected valeur de référence
   * @param actual   valeur lue dans le YAML
   * @return la liste des erreurs trouvées (vide si tout est valide)
   */
  private def valueErrors(path: String, expected: Any, actual: Any): List[String] =
    (expected, actual) match {
      case (e: Map[_, _], a: Map[_, _]) =>
        schemaErrors(e.asInstanceOf[Map[String, Any]], a.asInstanceOf[Map[String, Any]], s"$path.")
      case _ if typeMatches(expected, actual) => Nil
      case _ => List(s"type invalide pour $path : attendu $expected, reçu $actual")
    }

  /**
   * Compare les clés et les types d'une section de configuration avec le schéma de référence.
   *
   * @param expected schéma attendu (defaultConfig ou une de ses sous-sections)
   * @param actual   section lue dans le fichier YAML
   * @param prefix   préfixe du chemin des clés ("" à la racine, "app." pour une sous-section)
   * @return la liste des erreurs trouvées (vide si la section est valide)
   */
  private def schemaErrors(expected: Map[String, Any], actual: Map[String, Any], prefix: String = ""): List[String] = {
    val missing = (expected.keySet -- actual.keySet).toList.map(k => s"clé manquante : $prefix$k")
    val unknown = (actual.keySet -- expected.keySet).toList.map(k => s"clé inconnue : $prefix$k")
    val nested = expected.keySet.intersect(actual.keySet).toList
      .flatMap(k => valueErrors(s"$prefix$k", expected(k), actual(k)))
    missing ++ unknown ++ nested
  }

  /**
   * Aplatit l'arborescence pour retrouver directement "quarantine_path" ou "cbd_congestion_fee"
   *
   * @param map Arborescence type dictionnaire de dictionnaire
   * @return un dictionnaire plat clé : valeur
   */
  private def flatten(map: Map[String, Any]): Map[String, Any] = {
    map.flatMap {
      case (_, subMap: Map[_, _]) => flatten(subMap.asInstanceOf[Map[String, Any]])
      case (k, v) => Map(k -> v)
    }
  }

  /**
   * Bascule sur la configuration par défaut et trace la raison du rejet.
   *
   * @param reason message expliquant pourquoi le fichier a été rejeté
   * @return la configuration par défaut
   */
  private def fallbackToDefault(reason: String): Map[String, Any] = {
    logger.warn(s"Configuration rejetée, bascule sur la config par défaut : $reason")
    rejected = true
    defaultConfig
  }
}