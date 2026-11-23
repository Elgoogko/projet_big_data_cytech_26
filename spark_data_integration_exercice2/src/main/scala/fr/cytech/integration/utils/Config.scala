package fr.cytech.integration.utils

import org.yaml.snakeyaml.Yaml

import scala.io.Source
import scala.jdk.CollectionConverters._
import scala.util.{Try, Using}

import java.util.{List => JList, Map => JMap}

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
  }

  // Schéma de référence pour la validation de la configuration et configuration par défaut si config invalide
  private val defaultConfig: Map[String, Any] = Map(
    "filters_thresholds" -> Map(
      "fare_amount" -> 0.0,
      "extra" -> 0.0,
      "mta_tax" -> 0.0,
      "tip_amount" -> 0.0,
      "tolls_amount" -> 0.0,
      "improvement_surcharge" -> 0.0,
      "total_amount" -> 0.0,
      "congestion_surcharge" -> 0.0,
      "Airport_fee" -> 0.0,
      "cbd_congestion_fee" -> 0.0
    ),
    "app" -> Map(
      "spark" -> Map(
        "app_name" -> "spark_data_integration_exercice2",
        "master" -> "local[*]",
        "local_copy" -> true
      ),
      "s3" -> Map(
        "endpoint" -> "http://localhost:9000",
        "nyc_bucket" -> "s3a://nyc-bucket/"
  "raw_path" -> "nyc_raw"
  ,
  "processed_path" -> "nyc_processed"
  ,
        "parquets" -> List("yellow_tripdata_2026-07.parquet")
      ),
      "data_processing" -> Map(
        "quarantine" -> true,
        "quarantine_path" -> "nyc_quarantine"
      )
  ,
  "local_paths" -> Map(
    "raw_path" -> "data/raw",
    "processed_path" -> "data/processed",
    "quarantine_path" -> "data/quarantine"
  )
    )
  )

  // Chemin vers le fichier de configuration, par défaut redirigé vers le fichier default_config.yaml
  private var configPath: String = "src/main/resources/default_config.yaml"
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
    configPath = path
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
  private def loadConfig(path: String): Map[String, Any] = {
    val parsed = Using(Source.fromFile(path)) { source =>
      val yaml = new Yaml()
      val raw = yaml.load[Any](source.mkString)
      toScala(raw) match {
        case m: Map[_, _] => m.asInstanceOf[Map[String, Any]]
        case _ => Map.empty[String, Any]
      }
    }

    parsed.toOption
      .filter(cfg => matchesSchema(defaultConfig, cfg))
      .getOrElse {
        logger.warn(s"$path' introuvable ou structure invalide. Bascule sur la config par défaut.")
        rejected = true
        defaultConfig
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
   * Vérifie récursivement que chaque clé de la config par défaut existe dans le YAML
   * avec un type compatible (et sans clés inconnues)
   *
   * @param expected Schéma attendu
   * @param actual   Schéma actuellement chargé
   * @return True si les schémas correspondent, false sinon
   */
  private def matchesSchema(expected: Map[String, Any], actual: Map[String, Any]): Boolean = {
    expected.keySet == actual.keySet && expected.forall { case (key, expectedVal) =>
      (expectedVal, actual(key)) match {
        case (expMap: Map[_, _], actMap: Map[_, _]) =>
          matchesSchema(expMap.asInstanceOf[Map[String, Any]], actMap.asInstanceOf[Map[String, Any]])
        case (_: Double, _: Number) => true // YAML peut lire "0" comme Int ou "0.0" comme Double
        case (_: Int, _: Int) => true
        case (_: Boolean, _: Boolean) => true
        case (_: String, _: String) => true
        case (_: List[_], _: List[_]) => true
        case _ => false
      }
    }
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
}