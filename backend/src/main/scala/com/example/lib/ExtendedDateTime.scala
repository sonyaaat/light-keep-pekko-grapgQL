package com.example.lib

import org.apache.pekko.http.scaladsl.model.DateTime

import java.time.{ LocalDateTime, ZoneOffset }

object ExtendedDateTime {
  implicit class ExtendedDateTime(dt: DateTime) {
    def toLocalDateTime: LocalDateTime =
      LocalDateTime.ofEpochSecond(dt.clicks / 1000, 0, ZoneOffset.UTC)
  }
}
