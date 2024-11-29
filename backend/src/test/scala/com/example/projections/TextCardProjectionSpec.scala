package com.example.projections
import com.example.actors.TextCard.{Event, TextCardId}
import com.example.actors.{TextCard, TextCardTags}
import org.apache.pekko
import org.apache.pekko.persistence.query.Offset
import org.apache.pekko.{Done, NotUsed}
import org.apache.pekko.projection.ProjectionId
import org.apache.pekko.projection.eventsourced.EventEnvelope
import org.apache.pekko.stream.scaladsl.Source
import pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import pekko.projection.testkit.scaladsl.{ProjectionTestKit, TestProjection, TestSourceProvider}
import org.scalatest.wordspec.AnyWordSpecLike
import scalikejdbc.config.DBs

import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global


class TextCardProjectionSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  import com.example.repository.TextCardRepositoryImpl
  private val projectionTestKit = ProjectionTestKit(system)
  val projectionId: ProjectionId = ProjectionId("text-card", TextCardTags.Tag)
  val textCardUUID: UUID = UUID.randomUUID()

  val textCardRepository = new TextCardRepositoryImpl()

  val handler = new TextCardProjectionHandler(TextCardTags.Tag, system, textCardRepository)
  val testData: Source[(Int, TextCardId), NotUsed] = Source((0, UUID.randomUUID().toString) :: (1, UUID.randomUUID().toString) :: Nil)
  val extractOffset: ((Int, TextCardId)) => Int = (envelope: (Int, TextCardId)) => envelope._1
//  val sourceProvider: TestSourceProvider[Int, (Int, TextCardId)] = TestSourceProvider(testData, extractOffset)
//  val projection: TestProjection[Int, Object] = TestProjection(projectionId, sourceProvider, () => handler)

  private def createEnvelope(event: TextCard.Event, seqNo: Long, timestamp: Long = 0L) =
    EventEnvelope(Offset.sequence(seqNo), "persistenceId", seqNo, event, timestamp)


  DBs.setupAll()

  "ProjectionTestKit" must {
    "create a text card projection 10 times" in {
//      projectionTestKit.run(projection) {
//        textCardRepository.create(textCardUUID, LocalDateTime.now())
//      }
    }

    "get one text card projection" in {
      val events: List[EventEnvelope[Event]] =
        List(createEnvelope(TextCard.Created(textCardUUID.toString), 1))

      val sourceProvider: TestSourceProvider[Offset, EventEnvelope[TextCard.Event]] =
        TestSourceProvider[Offset, EventEnvelope[TextCard.Event]](
          Source(events),
          extractOffset = env => env.offset)
      val projection: TestProjection[Offset, EventEnvelope[TextCard.Event]] =
        TestProjection[Offset, EventEnvelope[TextCard.Event]](
          projectionId,
          sourceProvider,
          () => handler)

      projectionTestKit.run(projection) {
        textCardRepository.getOne(textCardUUID).futureValue
      }
    }

    "update title in text card projection" in {
      val sourceProvider: TestSourceProvider[Offset, EventEnvelope[TextCard.Event]] =
        TestSourceProvider[Offset, EventEnvelope[TextCard.Event]](
          Source(List(createEnvelope(TextCard.Created(textCardUUID.toString), 1), createEnvelope(TextCard.TitleUpdated(textCardUUID.toString, "title"), 2))),
          extractOffset = _.offset)
      val projection: TestProjection[Offset, EventEnvelope[TextCard.Event]] =
        TestProjection[Offset, EventEnvelope[TextCard.Event]](
          projectionId,
          sourceProvider,
          () => handler)

      projectionTestKit.run(projection) {
        textCardRepository.updateTitle(textCardUUID, "title").futureValue
      }
    }

    "update content in text card projection" in {
      val sourceProvider: TestSourceProvider[Offset, EventEnvelope[TextCard.Event]] =
        TestSourceProvider[Offset, EventEnvelope[TextCard.Event]](
          Source(List(createEnvelope(TextCard.Created(textCardUUID.toString), 1), createEnvelope(TextCard.ContentUpdated(textCardUUID.toString, "title"), 1))),
          extractOffset = env => env.offset)
      val projection: TestProjection[Offset, EventEnvelope[TextCard.Event]] =
        TestProjection[Offset, EventEnvelope[TextCard.Event]](
          projectionId,
          sourceProvider,
          () => handler)

      projectionTestKit.run(projection) {
        textCardRepository.updateContent(textCardUUID, "content").futureValue
      }

    }


    "delete text card projection" in {
      val sourceProvider: TestSourceProvider[Offset, EventEnvelope[TextCard.Event]] =
        TestSourceProvider[Offset, EventEnvelope[TextCard.Event]](
          Source(List(createEnvelope(TextCard.Created(textCardUUID.toString), 1), createEnvelope(TextCard.Deleted(textCardUUID.toString), 1))),
          extractOffset = env => env.offset)
      val projection: TestProjection[Offset, EventEnvelope[TextCard.Event]] =
        TestProjection[Offset, EventEnvelope[TextCard.Event]](
          projectionId,
          sourceProvider,
          () => handler)

      projectionTestKit.run(projection) {
        textCardRepository.delete(textCardUUID).futureValue
      }

    }

//    "expect done" in {
//      projectionTestKit.runWithTestSink(projection) { sinkProbe =>
//        sinkProbe.request(1)
//        sinkProbe.expectNext(Done)
//      }
//    }
  }
}
