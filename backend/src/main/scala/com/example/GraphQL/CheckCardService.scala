package com.example.GraphQL

import caliban.CalibanError
import com.example.NotFoundError
import com.example.actors.CheckCard.{CheckCardData, CheckCardId, ItemId}
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.util.Timeout
import com.example.actors.CheckCard
import com.example.repository.{CheckCardRecord, CheckCardRepository, CheckCardRepositoryImpl, ItemRecord}
import zio.{Hub, Task, ZIO, ZLayer}

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import com.example.dto.CheckCard.CheckCardIntegrationEvents._
import zio.stream.ZStream
import com.example.dto.CheckCard.Conversions._
import com.example.dto.CheckCard.{CheckCardDTO, CheckCardItemDTO}

trait CheckCardService {
  def getOne(checkCardId: CheckCardId): Task[Option[CheckCardRecord]]

  def getAllItemsByCheckCardId(checkCardId: CheckCardId): Task[List[ItemRecord]]

  def getAll: Task[List[CheckCardRecord]]

  def create(): Task[CheckCardDTO]

  def updateTitle(checkCardId: CheckCardId, title: String): Task[CheckCardData]

  def delete(checkCardId: CheckCardId): Task[CheckCardId]

  def appendItem(checkCardId: CheckCardId): Task[CheckCardItemDTO]

  def updateItemContent(checkCardId: CheckCardId, itemId: String, content: String): Task[CheckCardItemDTO]

  def removeItem(checkCardId: CheckCardId, itemId: ItemId): Task[ItemId]

  def checkItem(checkCardId: CheckCardId, itemId: ItemId): Task[CheckCardItemDTO]

  def unCheckItem(checkCardId: CheckCardId, itemId: ItemId): Task[CheckCardItemDTO]

  def checkCardEvent: ZStream[Any, Nothing, Event]
}


object CheckCardService {
  def make(implicit system: ActorSystem[_]): ZLayer[Any, Nothing, CheckCardService] =
    ZLayer {

      for {
        eventHub <- Hub.unbounded[Event]
      } yield new CheckCardService {
        implicit val timeout: Timeout = 10.seconds
        implicit val ec: ExecutionContext = system.executionContext
        implicit val checkCardRepo: CheckCardRepository = new CheckCardRepositoryImpl()

        override def getOne(checkCardId: CheckCardId): Task[Option[CheckCardRecord]] = {
          implicit val checkCardRepo: CheckCardRepository = new CheckCardRepositoryImpl()
          ZIO.fromFuture { _ =>
            checkCardRepo.getOne(UUID.fromString(checkCardId))
          }
        }.mapError(error => new NotFoundError(error.toString))

        override def getAllItemsByCheckCardId(checkCardId: CheckCardId): Task[List[ItemRecord]] = {
          ZIO.fromFuture { _ =>
            checkCardRepo.getAllItemsByCheckCardId(UUID.fromString(checkCardId))
          }
        }.mapError(error => new NotFoundError(error.toString))

        override def getAll: Task[List[CheckCardRecord]] = {
          ZIO.fromFuture { _ =>
            checkCardRepo.getAll()
          }
        }.mapError(error => new NotFoundError(error.toString))

        override def create(): Task[CheckCardDTO] =
          ZIO.fromFuture { _ =>
              val checkCardId: CheckCardId = UUID.randomUUID().toString
              CheckCard.getEntityRef(checkCardId).askWithStatus(CheckCard.Create)
            }
            .tap(checkCardData => eventHub.publish(Created(checkCardData))).mapBoth(error => new NotFoundError(error.toString), identity(_))

        override def updateTitle(checkCardId: CheckCardId, title: String): Task[CheckCardData] =
          ZIO.fromFuture { _ =>
              CheckCard.getEntityRef(checkCardId).askWithStatus(ref => CheckCard.UpdateTitle(ref, title))
            }
            .tap(checkCardData => eventHub.publish(TitleUpdated(checkCardData.checkCardId, checkCardData.title.get)))
            //            .mapBoth(error => new NotFoundError(error.toString), identity(_))
            .mapError(error => new NotFoundError(error.toString))

        override def delete(checkCardId: CheckCardId): Task[CheckCardId] =
          ZIO.fromFuture { _ =>
              CheckCard.getEntityRef(checkCardId).askWithStatus(CheckCard.Delete)
            }
            .tap(checkCardId => eventHub.publish(Deleted(checkCardId)))
            .mapError(error => new NotFoundError(error.toString))

        override def appendItem(checkCardId: CheckCardId): Task[CheckCardItemDTO] =
          ZIO.fromFuture { _ =>
              CheckCard.getEntityRef(checkCardId).askWithStatus(CheckCard.AppendItem)
            }
            .tap(itemData => eventHub.publish(ItemAppended(itemData)))
            .mapBoth(error => new NotFoundError(error.toString), identity(_))

        override def updateItemContent(checkCardId: CheckCardId, itemId: ItemId, content: String): Task[CheckCardItemDTO] =
          ZIO.fromFuture { _ =>
              CheckCard.getEntityRef(checkCardId).askWithStatus(ref => CheckCard.UpdateItemContent(ref, itemId, content))
            }
            .tap(itemData => eventHub.publish(ItemUpdated(itemData.itemId, itemData.content.get)))
            .mapBoth(error => new NotFoundError(error.toString), identity(_))

        override def removeItem(checkCardId: CheckCardId, itemId: ItemId): Task[ItemId] =
          ZIO.fromFuture { _ =>
              CheckCard.getEntityRef(checkCardId).askWithStatus(ref=>CheckCard.DeleteItem(ref,itemId))
            }
            .tap(itemId => eventHub.publish(ItemDeleted(itemId)))
            .mapError(error => new NotFoundError(error.toString))

        override def checkItem(checkCardId: CheckCardId, itemId: ItemId): Task[CheckCardItemDTO] =
          ZIO.fromFuture { _ =>
              CheckCard.getEntityRef(checkCardId).askWithStatus(ref => CheckCard.CheckItem(ref, itemId))
            }
            .tap(itemData => eventHub.publish(ItemChecked(itemData.itemId)))
            .mapBoth(error => new NotFoundError(error.toString), identity(_))

        override def unCheckItem(checkCardId: CheckCardId, itemId: ItemId): Task[CheckCardItemDTO] =
          ZIO.fromFuture { _ =>
              CheckCard.getEntityRef(checkCardId).askWithStatus(ref => CheckCard.UncheckItem(ref, itemId))
            }
            .tap(itemData => eventHub.publish(ItemUnchecked(itemData.itemId)))
            .mapBoth(error => new NotFoundError(error.toString), identity(_))

        def checkCardEvent: ZStream[Any, Nothing, Event] =
          ZStream
            .scoped(
              eventHub.subscribe
            )
            .flatMap(ZStream.fromQueue(_))
      }
    }
}
