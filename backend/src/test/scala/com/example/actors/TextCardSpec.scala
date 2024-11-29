//package com.example.actors

import com.example.actors.TextCard._
import com.example.actors.TextCard
import com.typesafe.config.ConfigFactory
import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.apache.pekko.pattern.StatusReply
import org.apache.pekko.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID

class TextCardSpec
  extends ScalaTestWithActorTestKit(
    EventSourcedBehaviorTestKit.config.withFallback(ConfigFactory.load("application-test.conf"))
  ) with AnyWordSpecLike with BeforeAndAfterEach {

  private val textCardId = UUID.randomUUID().toString
  private val testString = "testString"
  private val eventSourcedTestKit =
    EventSourcedBehaviorTestKit[Command[_], Event, State](
      system,
      TextCard(textCardId)
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    eventSourcedTestKit.clear()
  }

  "TextCard" should {

    "be created" in {
      val result = eventSourcedTestKit.runCommand(Create)
      result.reply should matchPattern { case StatusReply.Success(TextCardData(`textCardId`, _, None, None)) => }
      result.event should matchPattern { case Created(`textCardId`) => }
      result.stateOfType[CreatedState] should matchPattern { case CreatedState(TextCardData(`textCardId`, _, None, None)) =>
      }
    }

    "be edited after creation" in {
      val result = eventSourcedTestKit.runCommand(Create)
      result.reply should matchPattern { case StatusReply.Success(TextCardData(`textCardId`, _, None, None)) => }
      result.event should matchPattern { case Created(`textCardId`) => }
      result.stateOfType[CreatedState] should matchPattern { case CreatedState(TextCardData(`textCardId`, _, None, None)) =>
      }

      val result2 = eventSourcedTestKit.runCommand(UpdateTitle(_, "newTitle"))
      result2.reply should matchPattern { case StatusReply.Success(TextCardData(`textCardId`, _, _, None)) => }
      result2.event should matchPattern { case TitleUpdated(`textCardId`, "newTitle") => }
      result2.stateOfType[CreatedState] should matchPattern { case CreatedState(TextCardData(`textCardId`, _, _, None)) => }

      val result3 = eventSourcedTestKit.runCommand(UpdateContent(_, "newContent"))
      result3.reply should matchPattern {
        case StatusReply.Success(
        TextCardData(`textCardId`, _, Some("newTitle"), Some("newContent"))) =>
      }
      result3.event should matchPattern { case ContentUpdated(`textCardId`, "newContent") => }
      result3.stateOfType[CreatedState] should matchPattern { case CreatedState(TextCardData(`textCardId`, _, _, _)) => }
    }

    "not be created twice" in {
      val result = eventSourcedTestKit.runCommand(Create)
      result.reply should matchPattern { case StatusReply.Success(TextCardData(textCardId, _, None, None)) => }
      result.event should matchPattern { case Created(`textCardId`) => }
      val result2 = eventSourcedTestKit.runCommand(Create)
      result2.reply.isError shouldBe true
    }
    "be deleted" in {
      val create = eventSourcedTestKit.runCommand(Create)
      val result = eventSourcedTestKit.runCommand(Delete)
      result.reply shouldBe StatusReply.Success(textCardId)
      result.event should matchPattern { case Deleted(`textCardId`) => }
      result.stateOfType[DeletedState]

    }

    "not accept apy commands in deleted state" in {
      val create = eventSourcedTestKit.runCommand(Create)
      val result = eventSourcedTestKit.runCommand(Delete)
      result.reply shouldBe StatusReply.Success(`textCardId`)
      result.stateOfType[DeletedState]

      val result2 = eventSourcedTestKit.runCommand(Create)
      result2.reply.isError shouldBe true

      val result3 = eventSourcedTestKit.runCommand(UpdateTitle(_, testString))
      result3.reply.isError shouldBe true

      val result4 = eventSourcedTestKit.runCommand(UpdateContent(_, testString))
      result4.reply.isError shouldBe true

      val result5 = eventSourcedTestKit.runCommand(Delete)
      result5.reply.isError shouldBe true

      val result6 = eventSourcedTestKit.runCommand(GetOne)
      result6.reply.isError shouldBe true
    }
    "not accept commands in not created state" in {
      val result3 = eventSourcedTestKit.runCommand(UpdateTitle(_, testString))
      result3.reply.isError shouldBe true

      val result4 = eventSourcedTestKit.runCommand(UpdateContent(_, testString))
      result4.reply.isError shouldBe true

      val result5 = eventSourcedTestKit.runCommand(Delete)
      result5.reply.isError shouldBe true

      val result6 = eventSourcedTestKit.runCommand(GetOne)
      result6.reply.isError shouldBe true
    }

    "allow getting info in all states" in {
      val result = eventSourcedTestKit.runCommand(Create)
      val result2 = eventSourcedTestKit.runCommand(GetOne)
      result2.reply should matchPattern { case StatusReply.Success(TextCardData(`textCardId`, _, None, None)) => }

      val result3 = eventSourcedTestKit.runCommand(UpdateTitle(_, "newTitle"))

      val result4 = eventSourcedTestKit.runCommand(GetOne)
      result4.reply should matchPattern { case StatusReply.Success(TextCardData(`textCardId`, _, Some("newTitle"), None)) => }

      val result5 = eventSourcedTestKit.runCommand(UpdateContent(_, "newContent"))
      result5.reply should matchPattern { case StatusReply.Success(
      TextCardData(`textCardId`, _, Some("newTitle"), Some("newContent"))
      ) =>
      }

      val result6 = eventSourcedTestKit.runCommand(GetOne)
      result6.reply should matchPattern { case StatusReply.Success(
      TextCardData(`textCardId`, _, Some("newTitle"), Some("newContent"))
      ) =>
      }
    }

  }

}
