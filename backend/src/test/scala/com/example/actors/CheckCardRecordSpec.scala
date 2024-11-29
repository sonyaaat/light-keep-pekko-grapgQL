package com.example.actors

import com.example.actors.CheckCard._
import com.typesafe.config.ConfigFactory
import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.apache.pekko.pattern.StatusReply
import org.apache.pekko.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID

class CheckCardRecordSpec
    extends ScalaTestWithActorTestKit(
      EventSourcedBehaviorTestKit.config.withFallback(ConfigFactory.load("application-test.conf"))
    ) with AnyWordSpecLike with BeforeAndAfterEach {

  private val checkCardId = UUID.randomUUID().toString
  private val randomItemId = UUID.randomUUID().toString
  private val title = "title"
  private val content = "content"

  private val eventSourcedTestKit =
    EventSourcedBehaviorTestKit[Command[_], Event, State](
      system,
      CheckCard(checkCardId)
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    eventSourcedTestKit.clear()
  }

  "CheckCard" should {
    "be created" in {
      val result = eventSourcedTestKit.runCommand(Create)
      result.reply should matchPattern { case StatusReply.Success(CheckCardData(`checkCardId`, _, None, List())) => }
      result.event should matchPattern { case Created(`checkCardId`) => }
      result.stateOfType[LiveState] should matchPattern { case LiveState(CheckCardData(`checkCardId`, _, None, List())) =>
      }
    }

    "not be created twice" in {
      eventSourcedTestKit.runCommand(Create)
      val result = eventSourcedTestKit.runCommand(Create)
      result.reply.isError shouldBe true
    }

    "update title" in {
      eventSourcedTestKit.runCommand(Create)
      val result = eventSourcedTestKit.runCommand(UpdateTitle(_, title))
      result.reply should matchPattern { case StatusReply.Success(CheckCardData(`checkCardId`, _, Some(`title`), List())) => }
      result.event should matchPattern { case TitleUpdated(`checkCardId`, `title`) => }
      result.stateOfType[LiveState] should matchPattern {
        case LiveState(CheckCardData(`checkCardId`, _, Some(`title`), List())) =>
      }
    }

    "show card info" in {
      eventSourcedTestKit.runCommand(Create)
      val result = eventSourcedTestKit.runCommand(GetOne)
      result.reply should matchPattern { case StatusReply.Success(CheckCardData(`checkCardId`, _, None, List())) => }
//      result.event shouldBe None
      result.stateOfType[LiveState] should matchPattern { case LiveState(CheckCardData(`checkCardId`, _, None, List())) =>
      }
    }

    "append item" in {
      eventSourcedTestKit.runCommand(Create)
      val result = eventSourcedTestKit.runCommand(AppendItem)

      val checkCard = eventSourcedTestKit.runCommand(GetOne)
      val itemId = checkCard.stateOfType[LiveState].checkCard.items.last.itemId

      result.reply should matchPattern { case StatusReply.Success(ItemData(`itemId`, _, None, false)) => }
      result.event should matchPattern { case ItemAppended(`itemId`, _) => }
      result.stateOfType[LiveState] should matchPattern {
        case LiveState(CheckCardData(`checkCardId`, _, None, List(ItemData(`itemId`, _, None, false)))) =>
      }
    }

    "update item content" in {
      eventSourcedTestKit.runCommand(Create)
      eventSourcedTestKit.runCommand(AppendItem)

      val checkCard = eventSourcedTestKit.runCommand(GetOne)
      val itemId = checkCard.stateOfType[LiveState].checkCard.items.last.itemId

      val result = eventSourcedTestKit.runCommand(UpdateItemContent(_, itemId, content))
      result.reply should matchPattern { case StatusReply.Success(ItemData(`itemId`, _, Some(`content`), false)) => }
      result.event should matchPattern { case ItemUpdated(`itemId`, `content`) => }
      result.stateOfType[LiveState] should matchPattern {
        case LiveState(CheckCardData(`checkCardId`, _, None, List(ItemData(`itemId`, _, Some(`content`), false)))) =>
      }
    }

    "check item" in {
      eventSourcedTestKit.runCommand(Create)
      eventSourcedTestKit.runCommand(AppendItem)

      val checkCard = eventSourcedTestKit.runCommand(GetOne)
      val itemId = checkCard.stateOfType[LiveState].checkCard.items.last.itemId

      val result = eventSourcedTestKit.runCommand(CheckItem(_, itemId))
      result.reply should matchPattern { case StatusReply.Success(ItemData(`itemId`, _, None, true)) => }
      result.event should matchPattern { case ItemChecked(`itemId`) => }
      result.stateOfType[LiveState] should matchPattern {
        case LiveState(CheckCardData(`checkCardId`, _, None, List(ItemData(`itemId`, _, None, true)))) =>
      }
    }

    "uncheck item" in {
      eventSourcedTestKit.runCommand(Create)
      eventSourcedTestKit.runCommand(AppendItem)

      val checkCard = eventSourcedTestKit.runCommand(GetOne)
      val itemId = checkCard.stateOfType[LiveState].checkCard.items.last.itemId

      eventSourcedTestKit.runCommand(CheckItem(_, itemId))
      val result = eventSourcedTestKit.runCommand(UncheckItem(_, itemId))
      result.reply should matchPattern { case StatusReply.Success(ItemData(`itemId`, _, None, false)) => }
      result.event should matchPattern { case ItemUnchecked(`itemId`) => }
      result.stateOfType[LiveState] should matchPattern {
        case LiveState(CheckCardData(`checkCardId`, _, None, List(ItemData(`itemId`, _, None, false)))) =>
      }
    }

    "delete item" in {
      eventSourcedTestKit.runCommand(Create)
      eventSourcedTestKit.runCommand(AppendItem)

      val checkCard = eventSourcedTestKit.runCommand(GetOne)
      val itemId = checkCard.stateOfType[LiveState].checkCard.items.last.itemId

      val result = eventSourcedTestKit.runCommand(DeleteItem(_, itemId))
      result.reply shouldBe StatusReply.Success(itemId)
      result.event should matchPattern { case ItemDeleted(`itemId`) => }
      result.stateOfType[LiveState] should matchPattern { case LiveState(CheckCardData(`checkCardId`, _, None, List())) =>
      }
    }

    "not append item before check card creation" in {
      val result = eventSourcedTestKit.runCommand(AppendItem)
      result.reply.isError shouldBe true
    }

    "not update item title before its creation" in {
      eventSourcedTestKit.runCommand(Create)
      val result = eventSourcedTestKit.runCommand(UpdateItemContent(_, randomItemId, content))
      result.reply.isError shouldBe true
    }

    "not delete item title before its creation" in {
      eventSourcedTestKit.runCommand(Create)
      val result = eventSourcedTestKit.runCommand(DeleteItem(_, randomItemId))
      result.reply.isError shouldBe true
    }

    "not check item title before its creation" in {
      eventSourcedTestKit.runCommand(Create)
      val result = eventSourcedTestKit.runCommand(CheckItem(_, randomItemId))
      result.reply.isError shouldBe true
    }

    "not uncheck item title before its creation" in {
      eventSourcedTestKit.runCommand(Create)
      val result = eventSourcedTestKit.runCommand(UncheckItem(_, randomItemId))
      result.reply.isError shouldBe true
    }

    "append some items" in {
      eventSourcedTestKit.runCommand(Create)
      eventSourcedTestKit.runCommand(AppendItem)
      val result = eventSourcedTestKit.runCommand(AppendItem)

      val checkCard = eventSourcedTestKit.runCommand(GetOne)
      val itemId2 = checkCard.stateOfType[LiveState].checkCard.items.last.itemId
      val itemId1 = checkCard.stateOfType[LiveState].checkCard.items.head.itemId

      result.reply should matchPattern { case StatusReply.Success(ItemData(`itemId2`, _, None, false)) => }
      result.event should matchPattern { case _: ItemAppended => }
      result.stateOfType[LiveState] should matchPattern {
        case LiveState(
              CheckCardData(`checkCardId`, _, None, List(ItemData(`itemId1`, _, None, false), ItemData(`itemId2`, _, None, false)))
            ) =>
      }
    }

    "be deleted" in {
      eventSourcedTestKit.runCommand(Create)
      val result = eventSourcedTestKit.runCommand(Delete)
      result.reply shouldBe StatusReply.Success(`checkCardId`)
      result.event should matchPattern { case Deleted(`checkCardId`) => }
      result.stateOfType[DeletedState.type] should matchPattern { case DeletedState => }
    }

    "not be deleted twice" in {
      eventSourcedTestKit.runCommand(Create)
      eventSourcedTestKit.runCommand(Delete)
      val result = eventSourcedTestKit.runCommand(Delete)
      result.reply.isError shouldBe true
    }

    "not be updated before creation" in {
      val result = eventSourcedTestKit.runCommand(UpdateTitle(_, title))
      result.reply.isError shouldBe true
    }

    "not be deleted before creation" in {
      val result = eventSourcedTestKit.runCommand(Delete)
      result.reply.isError shouldBe true
    }

  }
}
