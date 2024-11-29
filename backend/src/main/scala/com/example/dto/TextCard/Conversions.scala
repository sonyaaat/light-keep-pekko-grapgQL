package com.example.dto.TextCard

import com.example.actors.TextCard.TextCardData
import com.example.repository.TextCardRecord

import scala.language.implicitConversions

object Conversions {
  implicit def textCardDTOfromData(d: TextCardData): TextCardDTO =
    TextCardDTO(
      textCardId = d.textCardId,
      createdAt = d.createdAt,
      title = d.title,
      content = d.content
    )

  implicit def textCardDTOfromRecord(r: TextCardRecord): TextCardDTO =
    TextCardDTO(
      textCardId = r.id.toString,
      createdAt = r.created_at,
      title = r.title,
      content = r.content
    )
}
