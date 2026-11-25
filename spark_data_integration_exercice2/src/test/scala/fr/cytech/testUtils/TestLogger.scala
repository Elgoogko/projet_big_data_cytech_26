package fr.cytech.testUtils

import org.slf4j.{Logger, LoggerFactory}

trait TestLogger {
  protected lazy val logger: Logger =
    LoggerFactory.getLogger(this.getClass.getName.stripSuffix("$"))
}