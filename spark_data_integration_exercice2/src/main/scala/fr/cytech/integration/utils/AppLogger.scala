package fr.cytech.integration.utils

import org.slf4j.{Logger, LoggerFactory}

trait AppLogger {
  protected lazy val logger: Logger =
    LoggerFactory.getLogger(this.getClass.getName.stripSuffix("$"))

  /**
   * Imprime un message si le verbose mode l'autorise.
   *
   * @param level niveau de gravité du message
   * @param scope portée du message
   * @param msg   message, évalué uniquement s'il est affiché
   */
  protected def log(level: LogLevel, scope: LogScope)(msg: => String): Unit =
    if (LogConfig.shouldPrint(level, scope)) level match {
      case LogLevel.Debug => logger.debug(msg)
      case LogLevel.Info => logger.info(msg)
      case LogLevel.Warn => logger.warn(msg)
      case LogLevel.Error => logger.error(msg)
    }

  /**
   * Raccourci pour les erreurs : toujours affichées, avec la cause.
   *
   * @param msg message d'erreur
   * @param t   exception à l'origine de l'erreur
   */
  protected def logError(msg: => String, t: Throwable): Unit =
    logger.error(msg, t)
}