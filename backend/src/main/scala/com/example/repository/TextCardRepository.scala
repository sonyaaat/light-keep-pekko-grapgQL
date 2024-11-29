package com.example.repository

import org.apache.pekko.Done
import scalikejdbc._
import zio.Task

import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

trait TextCardRepository {
  def create(textCardId: UUID, createdAt: LocalDateTime): Future[Done]

  def updateTitle(textCardId: UUID, title: String): Future[Done]
  def updateContent(textCardId: UUID, title: String): Future[Done]

  def getOne(textCardId: UUID): Future[Option[TextCardRecord]]
  def getAll(): Future[List[TextCardRecord]]

  def delete(textCardId: UUID): Future[Done]
}

class TextCardRepositoryImpl(implicit session: DBSession = AutoSession, ec: ExecutionContext)
    extends TextCardRepository {

  implicit val uuidFactory = ParameterBinderFactory[UUID] { value => (stmt, idx) =>
    stmt.setObject(idx, value)
  }

  override def create(textCardId: UUID, createdAt: LocalDateTime): Future[Done] =
    Future {
      sql"""
              INSERT INTO text_card (id, created_at) VALUES ($textCardId, $createdAt)

            """.update.apply()
      Done
    }

  override def updateTitle(textCardId: UUID, title: String): Future[Done] =
    Future {
      sql"""
            UPDATE text_card SET title = $title WHERE id = $textCardId
      """.update.apply()
      Done
    }
  override def updateContent(textCardId: UUID, content: String): Future[Done] = Future {
    sql"""
          UPDATE text_card SET content = $content WHERE id = $textCardId
    """.update.apply()
    Done
  }

  override def getOne(textCardId: UUID): Future[Option[TextCardRecord]] =
    Future {
      val t = TextCardRecord.syntax("t")
      withSQL {
        select
          .from[TextCardRecord](TextCardRecord as t)
          .where
          .eq(t.id, textCardId)
      }.map(TextCardRecord(t)).single().apply()
    }

  override def delete(textCardId: UUID): Future[Done] = Future {
    sql"""
          DELETE FROM text_card WHERE id = $textCardId
    """.update.apply()
    Done
  }

  override def getAll(): Future[List[TextCardRecord]] = Future {
    val t = TextCardRecord.syntax("t")
    withSQL {
      select
        .from[TextCardRecord](TextCardRecord as t)
    }.map(TextCardRecord(t)).toList().apply()
  }
}
