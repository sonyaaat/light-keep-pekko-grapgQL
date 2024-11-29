package com.example.repository

import java.time.LocalDateTime
import java.util.UUID
import org.apache.pekko.Done
import scalikejdbc.{AutoSession, DBSession, scalikejdbcSQLInterpolationImplicitDef}

import scala.concurrent.{ExecutionContext, Future}
import com.example.repository.{CheckCardRecord, ItemRecord}
import scalikejdbc._
final case class CheckCardResponse(checkCard: Option[CheckCardRecord], items: List[ItemRecord])
trait CheckCardRepository {
  def create(checkCardId: UUID, createdAt: LocalDateTime): Future[Done]

  def updateTitle(checkCardId: UUID, title: String): Future[Done]

  def getOne(checkCardId: UUID): Future[Option[CheckCardRecord]]

  def delete(checkCardId: UUID): Future[Done]

  def appendItem(checkCardId: UUID, created_at: LocalDateTime, itemId: UUID): Future[Done]

  def updateItemContent(itemId: UUID, content: String): Future[Done]

  def deleteItem(itemId: UUID): Future[Done]

  def checkItem(itemId: UUID): Future[Done]

  def uncheckItem(itemId: UUID): Future[Done]

  def getAll(): Future[List[CheckCardRecord]]

  def getAllItemsByCheckCardId(checkCardId: UUID): Future[List[ItemRecord]]
}

class CheckCardRepositoryImpl(implicit session: DBSession = AutoSession, ec: ExecutionContext)
  extends CheckCardRepository {
  implicit val uuidFactory = ParameterBinderFactory[UUID] { value =>
    (stmt, idx) =>
      stmt.setObject(idx, value)
  }


  override def create(checkCardId: UUID, createdAt: LocalDateTime): Future[Done] = {
    Future {
      sql"""
              INSERT INTO check_card (id, created_at) VALUES ($checkCardId, $createdAt)
            """.update.apply()
      Done
    }
  }

  override def updateTitle(checkCardId: UUID, title: String): Future[Done] = {
    Future {
      sql"""
            UPDATE check_card SET title = $title WHERE id = $checkCardId
      """
        .update.apply()
      Done
    }

  }

  //  override def getOne(checkCardId: UUID): Future[Option[Data]] = {
  //    Future {
  //      sql"""
  //             SELECT * FROM check_card WHERE id = $checkCardId
  //           """.map(res => {
  //        val checkCardIdString = res.string("id")
  //        val created_at = res.localDateTime("created_at")
  //        val title = res.stringOpt("title")
  //
  //        val check_card_items = sql"""
  //              SELECT * FROM check_card_item WHERE check_card_id = $checkCardId
  //             """.map(res =>
  //          Item(
  //            res.string("id"),
  //            res.localDateTime("created_at"),
  //            res.stringOpt("content"),
  //            res.boolean("is_checked")
  //
  //          )).list.apply()
  //        Data(checkCardIdString, created_at, title, check_card_items)
  //      }).single.apply()
  //
  //    }
  //  }
  override def getOne(checkCardId: UUID): Future[Option[CheckCardRecord]] = {
    Future {
//      val items: Future[List[Item]] =getAllItemsByCheckCardId(checkCardId)
//      items.onComplete(el=>println("items",el))
      val t = CheckCardRecord.syntax("t")
//      val checkCard: Option[CheckCard] =
        withSQL {
        select.from[CheckCardRecord](CheckCardRecord as t)
          .where
          .eq(t.id, checkCardId)
      }.map(CheckCardRecord(t)).single().apply()
//      new CheckCardResponse(checkCard,items)
//      com.example.actors.CheckCard.Data(checkCard.get(), created_at, title, check_card_items)
    }
  }

  def delete(checkCardId: UUID): Future[Done] = {
    Future {
      sql"""
           DELETE FROM check_card_item
      WHERE check_card_id = $checkCardId;

      DELETE FROM check_card
      WHERE id = $checkCardId;
           """.update.apply()
      Done
    }

  }

  def appendItem(checkCardId: UUID, created_at: LocalDateTime, itemId: UUID): Future[Done] = {
    Future {
      sql"""
           INSERT INTO check_card_item (id, created_at,check_card_id) VALUES ($itemId, $created_at,$checkCardId)
           """.update.apply()
      Done
    }
  }

  def updateItemContent(itemId: UUID, content: String): Future[Done] = {
    Future {
      sql"""
           UPDATE check_card_item SET content = $content  WHERE id = $itemId
           """.update.apply()
      Done
    }
  }


  def deleteItem(itemId: UUID): Future[Done] = {
    Future {
      sql"""
           DELETE FROM check_card_item WHERE id = $itemId
           """.update.apply()
      Done
    }
  }

  def checkItem(itemId: UUID): Future[Done] = {
    Future {
      sql"""
           UPDATE check_card_item SET is_checked = TRUE WHERE id = $itemId
           """.update.apply()
      Done
    }
  }


  def uncheckItem(itemId: UUID): Future[Done] = {
    Future {
      sql"""
           UPDATE check_card_item SET is_checked = FALSE WHERE id = $itemId
           """.update.apply()
      Done
    }
  }

  override def getAll(): Future[List[CheckCardRecord]] = Future {
    val t = CheckCardRecord.syntax("t")
    withSQL {
      select
        .from[CheckCardRecord](CheckCardRecord as t)
    }.map(CheckCardRecord(t)).toList().apply()

  }

  override def getAllItemsByCheckCardId(checkCardId: UUID): Future[List[ItemRecord]] = Future {
    println(s"getting items of $checkCardId CheckCard")
    val t = ItemRecord.syntax("t")
    withSQL {
      select
        .from[ItemRecord](ItemRecord as t)
        .where
        .eq(t.check_card_id, checkCardId)
    }.map(ItemRecord(t)).toList().apply()
  }
}
