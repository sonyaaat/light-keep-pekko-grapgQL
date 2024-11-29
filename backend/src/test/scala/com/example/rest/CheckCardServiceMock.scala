package com.example.rest

import com.example.actors.{CheckCard, TextCard}
import com.example.actors.CheckCard.{ItemData, CheckCardId, ItemId}
import com.example.actors.TextCard.TextCardId
import com.example.repository
import com.example.repository.{ItemRecord, CheckCardRecord}

import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class CheckCardServiceMock extends CheckCardService {
  val testCheckCardId = "555e4567-e89b-12d3-a456-426655440111"
  val testItemCheckCardId = "777e4567-e89b-12d3-a456-426655440777"

  import ExecutionContext.Implicits.global

  override def getOne(checkCardId: CheckCardId): Future[Option[CheckCardRecord]] = {
    println("KKK")
    Future {
      checkCardId match {
        case `testCheckCardId` => {
          println("222")
          Option(CheckCardRecord(UUID.fromString(testCheckCardId), LocalDateTime.now(),None))
        }
        //        case _ => {println("HHH")
        //          None}
      }
    }
  }

  override def getAllItemsByCheckCardId(checkCardId: CheckCardId): Future[List[repository.ItemRecord]] = {
    Future {
      checkCardId match {
        case `testCheckCardId` => {
          println("3")

          List(ItemRecord(UUID.fromString(testItemCheckCardId), LocalDateTime.now, None, is_checked = false, UUID.fromString(testCheckCardId)),ItemRecord(UUID.fromString(testItemCheckCardId), LocalDateTime.now, None, is_checked = false, UUID.fromString(testCheckCardId)))
        }
      }
    }
  } //TODO

  override def create(): Future[CheckCard.CheckCardData] = Future {
    CheckCard.CheckCardData(testCheckCardId, LocalDateTime.now, None)
  }

  override def updateTitle(checkCardId: CheckCardId, title: String): Future[CheckCard.CheckCardData] = Future {
    checkCardId match {
      case `testCheckCardId` => CheckCard.CheckCardData(testCheckCardId, LocalDateTime.now, Option(title))
      case _ => throw new RuntimeException("Unexpected checkCardId")
    }
  }

  override def delete(checkCardId: CheckCardId): Future[CheckCardId] =
    Future {
      checkCardId match {
        case `testCheckCardId` => testCheckCardId
        case _ => throw new RuntimeException("Unexpected checkCardId")
      }
    }

  override def appendItem(checkCardId: CheckCardId): Future[CheckCard.ItemData] = {
    Future {
      checkCardId match {
        case `testCheckCardId` => ItemData(testItemCheckCardId, LocalDateTime.now(), None, false)
        case _ => throw new RuntimeException("Unexpected checkCardId")
      }
    }
  }

  override def updateItemContent(checkCardId: CheckCardId, itemUuid: String, content: String): Future[CheckCard.ItemData] =
    Future {
      checkCardId match {
        case `testCheckCardId` => ItemData(testItemCheckCardId, LocalDateTime.now(), Some(content), false)
        case _ => throw new RuntimeException("Unexpected checkCardId")
      }
    }

  override def removeItem(checkCardId: CheckCardId, itemUuid: ItemId): Future[ItemId] = {
    Future {
      checkCardId match {
        case `testCheckCardId` => itemUuid match {
          case `testItemCheckCardId` => testItemCheckCardId
          case _ => throw new RuntimeException("Unexpected itemId")
        }

        case _ => throw new RuntimeException("Unexpected checkCardId")
      }
    }
  }

  override def checkItem(checkCardId: CheckCardId, itemUuid: String): Future[CheckCard.ItemData] =
    Future {
      checkCardId match {
        case `testCheckCardId` => ItemData(testItemCheckCardId, LocalDateTime.now(), None, true)
        case _ => throw new RuntimeException("Unexpected checkCardId")
      }
    }

  override def unCheckItem(checkCardId: CheckCardId, itemUuid: String): Future[CheckCard.ItemData] =
    Future {
      checkCardId match {
        case `testCheckCardId` => ItemData(testItemCheckCardId, LocalDateTime.now(), None, false)
        case _ => throw new RuntimeException("Unexpected checkCardId")
      }
    }
}

