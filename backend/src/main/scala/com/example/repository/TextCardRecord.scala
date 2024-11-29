package com.example.repository

import scalikejdbc._

import java.time.LocalDateTime
import java.util.UUID
import com.example.actors.TextCard.TextCardData

final case class TextCardRecord(
  id: UUID,
  title: Option[String],
  content: Option[String],
  created_at: LocalDateTime
)

object TextCardRecord extends SQLSyntaxSupport[TextCardRecord] {
  override def tableName: String = "text_card"

  def apply(t: SyntaxProvider[TextCardRecord])(rs: WrappedResultSet): TextCardRecord =
    apply(t.resultName)(rs)

  def apply(t: ResultName[TextCardRecord])(rs: WrappedResultSet): TextCardRecord =
    new TextCardRecord(
      id = UUID.fromString(rs.string(t.id)),
      title = rs.stringOpt(t.title),
      content = rs.stringOpt(t.content),
      created_at = rs.localDateTime(t.created_at)
    )

  def fromTextCardData(data: TextCardData): TextCardRecord =
    new TextCardRecord(
      id = UUID.fromString(data.textCardId),
      title = data.title,
      content = data.content,
      created_at = data.createdAt
    )
}
