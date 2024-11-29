package com.example.rest

import com.example.actors.CheckCard
import com.example.actors.CheckCard.{ItemData, CheckCardId, ItemId}
import com.example.repository.{CheckCardRepository, CheckCardRepositoryImpl, CheckCardRecord , ItemRecord }
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.util.Timeout

import java.util.UUID
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

trait CheckCardService {
  def getOne(checkCardId: CheckCardId): Future[Option[CheckCardRecord]]
  def getAllItemsByCheckCardId(checkCardId: CheckCardId):Future[List[ItemRecord]]

  def create(): Future[CheckCard.CheckCardData]

  def updateTitle(checkCardId: CheckCardId, title: String): Future[CheckCard.CheckCardData]

  def delete(checkCardId: CheckCardId): Future[CheckCardId]

  def appendItem(checkCardId: CheckCardId): Future[CheckCard.ItemData]

  def updateItemContent(checkCardId: CheckCardId, itemUuid: String, content: String): Future[CheckCard.ItemData]

  def removeItem(checkCardId: CheckCardId, itemUuid: String): Future[CheckCard.ItemId]

  def checkItem(checkCardId: CheckCardId, itemUuid: String): Future[CheckCard.ItemData]

  def unCheckItem(checkCardId: CheckCardId, itemUuid: String): Future[CheckCard.ItemData]
}


class CheckCardServiceImpl(implicit system: ActorSystem[_]) extends CheckCardService {
  implicit val timeout: Timeout = 10.seconds
  implicit val ec: ExecutionContext = system.executionContext

  override def getOne(checkCardId: CheckCardId): Future[Option[CheckCardRecord]] = {
    implicit val checkCardRepo: CheckCardRepository = new CheckCardRepositoryImpl()
    checkCardRepo.getOne(UUID.fromString(checkCardId))
  }

  override def getAllItemsByCheckCardId(checkCardId: CheckCardId): Future[List[ItemRecord]] = {
    implicit val checkCardRepo: CheckCardRepository = new CheckCardRepositoryImpl()
    checkCardRepo.getAllItemsByCheckCardId(UUID.fromString(checkCardId))
  }

  override def create: Future[CheckCard.CheckCardData] = {
    val checkCardId: CheckCard.CheckCardId = UUID.randomUUID().toString
    CheckCard.getEntityRef(checkCardId).askWithStatus(CheckCard.Create)
  }

  override def updateTitle(checkCardId: CheckCardId, title: CheckCardId): Future[CheckCard.CheckCardData] = {
    CheckCard.getEntityRef(checkCardId).askWithStatus(ref => CheckCard.UpdateTitle(ref, title))
  }

  override def delete(checkCardId: CheckCardId): Future[CheckCardId] = {
    CheckCard.getEntityRef(checkCardId).askWithStatus(CheckCard.Delete)
  }

  override def appendItem(checkCardId: CheckCardId): Future[ItemData] = {
    CheckCard.getEntityRef(checkCardId).askWithStatus(CheckCard.AppendItem)
  }

  override def updateItemContent(checkCardId: CheckCardId, itemUuid: CheckCardId, content: CheckCardId): Future[ItemData] = {
    CheckCard.getEntityRef(checkCardId).askWithStatus(ref => CheckCard.UpdateItemContent(ref, itemUuid, content))
  }

  override def removeItem(checkCardId: CheckCardId, itemUuid: ItemId): Future[ItemId] = {
    CheckCard.getEntityRef(checkCardId).askWithStatus(ref => CheckCard.DeleteItem(ref, itemUuid))
  }

  override def checkItem(checkCardId: CheckCardId, itemUuid: CheckCardId): Future[ItemData] = {
    CheckCard.getEntityRef(checkCardId).askWithStatus(ref => CheckCard.CheckItem(ref, itemUuid))
  }

  override def unCheckItem(checkCardId: CheckCardId, itemUuid: CheckCardId): Future[ItemData] = {
    CheckCard.getEntityRef(checkCardId).askWithStatus(ref => CheckCard.UncheckItem(ref, itemUuid))
  }


}
