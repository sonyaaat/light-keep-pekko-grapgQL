package com.example.repository
import scalikejdbc._

import java.time.LocalDateTime
import java.util.UUID

final case class CheckCardRecord(
                            id: UUID,
                            created_at: LocalDateTime,
                            title: Option[String] = None
                          )



object CheckCardRecord extends SQLSyntaxSupport[CheckCardRecord] {
  override def tableName: String = "check_card"

  def apply(t: SyntaxProvider[CheckCardRecord])(rs: WrappedResultSet): CheckCardRecord =
    apply(t.resultName)(rs)

  def apply(t: ResultName[CheckCardRecord])(rs: WrappedResultSet): CheckCardRecord =
    new CheckCardRecord(
      id = UUID.fromString(rs.string(t.id)),
      title = rs.stringOpt(t.title),
      created_at = rs.localDateTime(t.created_at)
    )
}
