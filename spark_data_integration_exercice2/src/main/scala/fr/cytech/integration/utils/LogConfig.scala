package fr.cytech.integration.utils

/** Configuration globale du verbose mode, définie une fois au démarrage. */
object LogConfig {

  @volatile private var verbosity: Verbosity = Verbosity.N

  /**
   * Définit le niveau de verbosité du programme.
   *
   * @param raw lettre du verbose mode ("N", "M" ou "F")
   */
  def setVerbosity(raw: String): Unit = verbosity = Verbosity.parse(raw)

  def getVerbosity: Verbosity = verbosity

  /**
   * Décide si un message doit être imprimé.
   *
   * @param level niveau de gravité du message
   * @param scope portée du message
   * @return true si le message doit être affiché
   */
  def shouldPrint(level: LogLevel, scope: LogScope): Boolean =
    (level, scope) match {
      case (LogLevel.Error, _) => true // les erreurs passent toujours
        // case (LogLevel.Warn, _) => true                        // exemple : forcer les warn
      case (_, s) => verbosity.rank >= s.minRank
    }
}
