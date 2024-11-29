package com.example.repository
import scalikejdbc._

import java.time.LocalDateTime
import java.util.UUID
final case class ItemRecord(
                       id: UUID,
                       created_at: LocalDateTime,
                       content: Option[String] = None,
                       is_checked: Boolean = false,
                       check_card_id: UUID
                     )

object ItemRecord extends SQLSyntaxSupport[ItemRecord] {
  override def tableName: String = "check_card_item"

  def apply(t: SyntaxProvider[ItemRecord])(rs: WrappedResultSet): ItemRecord =
    apply(t.resultName)(rs)

  def apply(t: ResultName[ItemRecord])(rs: WrappedResultSet): ItemRecord =
    new ItemRecord(
      id = UUID.fromString(rs.string(t.id)),
      check_card_id = UUID.fromString(rs.string(t.check_card_id)),
      content = rs.stringOpt(t.content),
      created_at = rs.localDateTime(t.created_at),
      is_checked = rs.boolean(t.is_checked)
    )
}