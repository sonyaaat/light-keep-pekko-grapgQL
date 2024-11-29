package com.example.projections

import com.example.actors.CheckCard._
import com.example.actors.CheckCard.Event
import com.example.repository.CheckCardRepository
import org.apache.pekko.Done
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.projection.eventsourced.EventEnvelope
import org.apache.pekko.projection.scaladsl.Handler

import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

object CheckCardProjectionHandler {
  val LogInterval = 10
}
class CheckCardProjectionHandler(tag: String, system: ActorSystem[_], repo: CheckCardRepository)
  extends Handler[EventEnvelope[Event]]() {
  private implicit val ec: ExecutionContext = system.executionContext

  override def process(envelope: EventEnvelope[Event]): Future[Done] = {
    val processed: Future[Done] = envelope.event match {
      case Created(id) => {
        println("CREATED")
        repo.create(UUID.fromString(id),  LocalDateTime.ofInstant(Instant.ofEpochMilli(envelope.timestamp), ZoneOffset.UTC))
      }
      case TitleUpdated(checkCardId, title) => repo.updateTitle(UUID.fromString(checkCardId), title)
      case Deleted(checkCardId) => repo.delete(UUID.fromString(checkCardId))
      case ItemAppended(itemId, checkCardId) => repo.appendItem(UUID.fromString(checkCardId), LocalDateTime.ofInstant(Instant.ofEpochMilli(envelope.timestamp), ZoneOffset.UTC), UUID.fromString(itemId))
      case ItemUpdated(itemId, content) => repo.updateItemContent(UUID.fromString(itemId), content)
      case ItemDeleted(itemId) => repo.deleteItem(UUID.fromString(itemId))
      case ItemChecked(itemId) => repo.checkItem(UUID.fromString(itemId))
      case ItemUnchecked(itemId) => repo.uncheckItem(UUID.fromString(itemId))
      case _ => Future{
        println("UNEXPECTED EVENT IN CHECK CARD PROJECTION HANDLER")
        Done
      }
    }
    processed.onComplete {
      case Success(_) => println(envelope.event, "success!!Check")
      case _ => println("CHECK CARD PROJECTION HANDLER FAILED")
    }
    processed
  }

}

