package fr.cytech.integration.utils

/**
 * Niveau de verbosité choisi au lancement du programme.
 *
 * @param rank ordre croissant de verbosité (0 = N, 1 = M, 2 = F)
 */
sealed abstract class Verbosity(val rank: Int)

object Verbosity {
  case object N extends Verbosity(0)

  case object M extends Verbosity(1)

  case object F extends Verbosity(2)

  /**
   * Convertit la lettre reçue en niveau de verbosité.
   *
   * @param raw lettre brute ("N", "M" ou "F", insensible à la casse)
   * @return le niveau correspondant, N par défaut si la valeur est inconnue
   */
  def parse(raw: String): Verbosity = raw.toUpperCase match {
    case "M" => M
    case "F" => F
    case _ => N
  }
}

/**
 * Portée d'un message : décrit à quel moment du programme il appartient.
 *
 * @param minRank verbosité minimale pour que le message soit affiché
 */
sealed abstract class LogScope(val minRank: Int)

object LogScope {
  case object Lifecycle extends LogScope(0)

  case object Step extends LogScope(1)

  case object Detail extends LogScope(2)
}

/** Niveau de gravité d'un message, indépendant du verbose mode. */
sealed trait LogLevel

object LogLevel {
  case object Debug extends LogLevel

  case object Info extends LogLevel

  case object Warn extends LogLevel

  case object Error extends LogLevel
}