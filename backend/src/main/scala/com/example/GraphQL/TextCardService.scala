package com.example.GraphQL

import caliban.CalibanError

import com.example.actors.TextCard
import com.example.actors.TextCard.TextCardId
import com.example.repository.{ TextCardRepository, TextCardRepositoryImpl }
import com.example.dto.TextCard.TextCardIntegrationEvents._
import com.example.dto.TextCard.TextCardDTO
import com.example.dto.TextCard.Conversions._

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.util.Timeout

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

import zio.{ Hub, Task, ZIO, ZLayer }
import zio.stream.ZStream

trait TextCardService {
  def getOne(textCardId: TextCard.TextCardId): Task[Option[TextCardDTO]]
  def create: Task[TextCardDTO]
  def updateTitle(textCardId: TextCard.TextCardId, title: String): Task[TextCardDTO]
  def updateContent(textCardId: TextCard.TextCardId, content: String): Task[TextCardDTO]
  def delete(textCardId: TextCard.TextCardId): Task[TextCardId]
  def getAll: Task[List[TextCardDTO]]
  def onTextCardEvent: ZStream[Any, Nothing, Event]
}

object TextCardService {
  def make(implicit system: ActorSystem[_]): ZLayer[Any, Nothing, TextCardService] =
    ZLayer {

      for {
        eventHub <- Hub.unbounded[Event]
      } yield new TextCardService {
        implicit val timeout: Timeout = 10.seconds
        implicit val ec: ExecutionContext = system.executionContext
        implicit val textCardRepo: TextCardRepository = new TextCardRepositoryImpl()

        override def create: Task[TextCardDTO] = {
          for {
            textCard <- ZIO.fromFuture { _ =>
                          val textCardId: TextCardId = UUID.randomUUID().toString
                          TextCard.getEntityRef(textCardId).askWithStatus(TextCard.Create)
                        }
            _        <- eventHub.publish(Created(textCard))

          } yield textCard
        }.mapBoth(error => CalibanError.ExecutionError(error.toString), identity(_))

        override def updateTitle(textCardId: TextCardId, title: String): Task[TextCardDTO] = {
          for {
            textCard <- ZIO
                          .fromFuture { _ =>
                            TextCard.getEntityRef(textCardId).askWithStatus(ref => TextCard.UpdateTitle(ref, title))
                          }
            _        <- eventHub.publish(TitleUpdated(textCard.textCardId, textCard.title.get))
          } yield textCard
        }.mapBoth(error => CalibanError.ExecutionError(error.toString), identity(_))

        override def updateContent(textCardId: TextCardId, content: String): Task[TextCardDTO] = {
          for {
            textCard <- ZIO
                          .fromFuture { _ =>
                            TextCard
                              .getEntityRef(textCardId)
                              .askWithStatus(ref => TextCard.UpdateContent(ref, content))
                          }
            _        <- eventHub.publish(ContentUpdated(textCard.textCardId, textCard.content.get))
          } yield textCard
        }.mapBoth(error => CalibanError.ExecutionError(error.toString), identity(_))

        override def delete(textCardId: TextCardId): Task[TextCardId] = {
          for {
            textCardId <- ZIO
                            .fromFuture { _ =>
                              TextCard.getEntityRef(textCardId).askWithStatus(TextCard.Delete)
                            }
            _          <- eventHub.publish(Deleted(textCardId))
          } yield textCardId

        }.mapError(error => CalibanError.ExecutionError(error.toString))

        override def getAll: Task[List[TextCardDTO]] =
          ZIO.fromFuture { _ =>
            textCardRepo
              .getAll()
              .map(_.map(r => r))
          }

        override def getOne(textCardId: TextCardId): Task[Option[TextCardDTO]] =
          ZIO
            .fromFuture { _ =>
              textCardRepo.getOne(UUID.fromString(textCardId))
            }
            .map(_.map(r => r))

        def onTextCardEvent: ZStream[Any, Nothing, Event] =
          ZStream
            .scoped(
              eventHub.subscribe
            )
            .flatMap(ZStream.fromQueue(_))

      }
    }
}
