package com.example.dto.TextCard

import caliban.schema.Annotations.GQLName

import java.time.LocalDateTime

@GQLName("TextCard")
case class TextCardDTO(
  textCardId: String,
  createdAt: LocalDateTime,
  title: Option[String] = None,
  content: Option[String] = None
)
