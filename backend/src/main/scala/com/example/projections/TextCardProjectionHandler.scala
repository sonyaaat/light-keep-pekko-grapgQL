package com.example.projections

import com.example.actors.TextCard
import com.example.repository.TextCardRepository
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.projection.eventsourced.EventEnvelope
import org.apache.pekko.projection.scaladsl.Handler
import com.example.actors.TextCard._
import com.example.lib.ExtendedDateTime.ExtendedDateTime
import org.apache.pekko.Done
import org.apache.pekko.http.scaladsl.model.DateTime

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

object TextCardProjectionHandler {
  val LogInterval = 10
}

class TextCardProjectionHandler(tag: String, system: ActorSystem[_], repo: TextCardRepository)
    extends Handler[EventEnvelope[TextCard.Event]]() {
  private implicit val ec: ExecutionContext = system.executionContext

  override def process(envelope: EventEnvelope[Event]): Future[Done] = {
    val processed: Future[Done] = envelope.event match {
      case ContentUpdated(id, content) => repo.updateContent( UUID.fromString(id), content)
      case Created(id)                 =>
        println("CREATED")
        repo.create(UUID.fromString(id), DateTime(envelope.timestamp).toLocalDateTime)
      case Deleted(id)                 => repo.delete(UUID.fromString(id))
      case TitleUpdated(id, title)     => repo.updateTitle(UUID.fromString(id), title)
    }
    processed.onComplete {
      case Success(_) => println(envelope.event, "success!!")
      case _          => ()
    }
    processed
  }

}
