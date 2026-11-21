package fr.cytech.integration.utils

import org.slf4j.{Logger, LoggerFactory}

trait AppLogging {
  protected lazy val logger: Logger = LoggerFactory.getLogger(this.getClass.getName.stripSuffix("$"))
}