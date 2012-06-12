package com.gu.polls.util

import com.typesafe.config.ConfigFactory

object Config {
  val fallback = ConfigFactory.load()

  def get(key: String): String =
    configs.getOrElse(key, "")

  val configs = Map(
    "signing.key" -> fallback.getString("signing.key")
  )
}
