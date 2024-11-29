package com.example.actors

import com.example.lib.JsonSerializable
import org.apache.pekko.Done
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem, Behavior}
import org.apache.pekko.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef, EntityTypeKey}
import org.apache.pekko.http.scaladsl.model.DateTime
import org.apache.pekko.pattern.StatusReply
import org.apache.pekko.persistence.typed.{PersistenceId, RecoveryCompleted}
import org.apache.pekko.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect}

import java.time.{LocalDateTime}
import java.util.UUID


object CheckCardTags{
  val Tag = "check-card"
}

object CheckCard {
  type CheckCardId = String
  type ItemId = String
  case class CheckCardData(
                            checkCardId: CheckCardId,
                            createdAt: LocalDateTime,
                            title: Option[String] = None,
                            items: List[ItemData] = List()
                      )

  case class ItemData(
                       itemId: ItemId,
                       createdAt: LocalDateTime,
                       content: Option[String] = None,
                       isChecked: Boolean = false
                 )

  trait Command[F] extends JsonSerializable {
    def replyTo: ActorRef[StatusReply[F]]
  }
  // commands for a check card
  case class Create(replyTo: ActorRef[StatusReply[CheckCardData]]) extends Command[CheckCardData]
  case class GetOne(replyTo: ActorRef[StatusReply[CheckCardData]]) extends Command[CheckCardData]
  case class UpdateTitle(replyTo: ActorRef[StatusReply[CheckCardData]], title: String) extends Command[CheckCardData]
  case class Delete(replyTo: ActorRef[StatusReply[CheckCardId]]) extends Command[CheckCardId]
  // commands for an item
  case class AppendItem(replyTo: ActorRef[StatusReply[ItemData]]) extends Command[ItemData]
  case class UpdateItemContent(replyTo: ActorRef[StatusReply[ItemData]], itemId: ItemId, content: String) extends Command[ItemData]
  case class DeleteItem(replyTo: ActorRef[StatusReply[ItemId]], itemId: ItemId) extends Command[ItemId]
  case class CheckItem(replyTo: ActorRef[StatusReply[ItemData]], itemId: ItemId) extends Command[ItemData]
  case class UncheckItem(replyTo: ActorRef[StatusReply[ItemData]], itemId: ItemId) extends Command[ItemData]

  trait Event extends JsonSerializable
  // events for a check card
  case class Created(checkCardId: CheckCardId) extends Event
  case class TitleUpdated(checkCardId: CheckCardId, title: String) extends Event
  case class Deleted(checkCardId: CheckCardId) extends Event

  // events for an item
  case class ItemAppended(itemId: ItemId, checkCardId: CheckCardId) extends Event
  case class ItemUpdated(itemId: ItemId, content: String) extends Event
  case class ItemDeleted(itemId: ItemId) extends Event
  case class ItemChecked(itemId: ItemId) extends Event
  case class ItemUnchecked(itemId: ItemId) extends Event

  trait State extends JsonSerializable {
    def checkCard: CheckCardData = CheckCardData("", LocalDateTime.now)
  }
  private case object EmptyState extends State
  case class LiveState(override val checkCard: CheckCardData) extends State
  case object DeletedState extends State


  val TypeKey = EntityTypeKey[Command[_]]("CheckCard")

  def register(implicit system: ActorSystem[_]): Unit = {
    ClusterSharding(system).init(Entity(TypeKey)(context => CheckCard(context.entityId)))
  }

  def getEntityRef(id: String)(implicit system: ActorSystem[_]): EntityRef[Command[_]] =
    ClusterSharding(system).entityRefFor(TypeKey, id)

  def apply(checkCardId: CheckCardId): Behavior[Command[_]] = Behaviors.setup { context =>
    EventSourcedBehavior[Command[_], Event, State](
      persistenceId = PersistenceId.ofUniqueId(checkCardId),
      emptyState = EmptyState,
      commandHandler = commandHandler(context, checkCardId),
      eventHandler = eventHandler(context)
    )
      .receiveSignal {
        case (_, RecoveryCompleted) =>
          println("RECOVERED ALL")
      }
      .withTagger(_ => Set(CheckCardTags.Tag))
  }

  def commandHandler(context: ActorContext[Command[_]], checkCardId: CheckCardId)(state: State, command: Command[_]): Effect[Event, State] = {
    val errorMessage: String = s"Illegal command: $command for check card id: $checkCardId"
    state match {
      case EmptyState =>
        command match {
          case Create(replyTo) =>
            Effect
              .persist(Created(checkCardId))
              .thenReply(replyTo)(newState => StatusReply.success(newState.checkCard))
          case _ =>
            illegalCommandHandler(command, context, errorMessage)
        }
      case LiveState(data) =>
        command match {
          case Create(_) =>
            illegalCommandHandler(command, context, errorMessage)
          case GetOne(replyTo) =>
            Effect
              .none
              .thenReply(replyTo)(_ => StatusReply.success(data))
          case UpdateTitle(replyTo, title) =>
            persistEventWithReplyData(replyTo, TitleUpdated(checkCardId, title))
          case Delete(replyTo) =>
            Effect
              .persist(Deleted(checkCardId))
              .thenReply(replyTo)(_ => StatusReply.success(checkCardId))
          case AppendItem(replyTo) =>
            val itemId = UUID.randomUUID().toString
            persistEventWithReplyItem(replyTo, ItemAppended(itemId, checkCardId), itemId)
          case UpdateItemContent(replyTo, itemId, content) =>
            if (!isEmptyListOfItems(data, itemId)) persistEventWithReplyItem(replyTo, ItemUpdated(itemId, content), itemId)
            else illegalCommandHandler(command, context, errorMessage)
          case DeleteItem(replyTo, itemId) =>
            if (!isEmptyListOfItems(data, itemId))
              Effect
                .persist(ItemDeleted(itemId))
                .thenReply(replyTo)(_ => StatusReply.success(itemId))
            else illegalCommandHandler(command, context, errorMessage)
          case CheckItem(replyTo, itemId) =>
            if (!isEmptyListOfItems(data, itemId)) persistEventWithReplyItem(replyTo, ItemChecked(itemId), itemId)
            else illegalCommandHandler(command, context, errorMessage)
          case UncheckItem(replyTo, itemId) =>
            if(!isEmptyListOfItems(data, itemId)) persistEventWithReplyItem(replyTo, ItemUnchecked(itemId), itemId)
            else illegalCommandHandler(command, context, errorMessage)
        }
      case DeletedState =>
        illegalCommandHandler(command, context, errorMessage)
    }
  }
  def isEmptyListOfItems(data: CheckCardData, itemId: ItemId): Boolean = !data.items.exists(_.itemId == itemId)

  def illegalCommandHandler(command: Command[_], context: ActorContext[Command[_]], errorMessage: String): ReplyEffect[Nothing, State] = {
    context.log.error(errorMessage)
    Effect.none.thenReply(command.replyTo)((_: State)  => StatusReply.error(errorMessage))
  }

  def persistEventWithReplyData(replyTo: ActorRef[StatusReply[CheckCardData]], event: Event): Effect[Event, State]  ={
    Effect
      .persist(event)
      .thenReply(replyTo)(newState => StatusReply.success(newState.checkCard))
  }

  def persistEventWithReplyItem(replyTo: ActorRef[StatusReply[ItemData]], event: Event, itemId: ItemId): Effect[Event, State]  ={
    Effect
      .persist(event)
      .thenReply(replyTo)(newState => StatusReply.success(newState.checkCard.items.filter(_.itemId == itemId).head))
  }

  def eventHandler(context: ActorContext[Command[_]])(state: State, event: Event): State = {
    state match {
      case EmptyState =>
        event match {
          case event@Created(checkCardId) =>
            println(s"recovered $event")
            LiveState(CheckCardData(checkCardId, LocalDateTime.now, None, List()))
        }
      case LiveState(checkCardState) =>
        event match {
          case TitleUpdated(_, title) =>
            println(s"recovered $event")
            LiveState(CheckCardData(checkCardState.checkCardId, LocalDateTime.now, Option(title), checkCardState.items))
          case Deleted(_) =>
            println(s"recovered $event")
            DeletedState
          case ItemAppended(itemId, checkCardId) =>
            println(s"recovered $event")
            LiveState(CheckCardData(checkCardState.checkCardId, checkCardState.createdAt, checkCardState.title,
              checkCardState.items :+ ItemData(itemId, LocalDateTime.now, None)))
          case ItemUpdated(itemId, content) =>
            updateItemList(checkCardState, itemId, Option(content), checkCardState.items.filter(_.itemId == itemId).head.isChecked)
          case ItemDeleted(itemId) =>
            println(s"recovered $event")
            LiveState(CheckCardData(checkCardState.checkCardId, checkCardState.createdAt, checkCardState.title,
              checkCardState.items.filter(_.itemId != itemId)))
          case ItemChecked(itemId) =>
            updateItemList(checkCardState, itemId, checkCardState.items.filter(_.itemId == itemId).head.content, isChecked = true)
          case ItemUnchecked(itemId) =>
            updateItemList(checkCardState, itemId, checkCardState.items.filter(_.itemId == itemId).head.content)
        }
    }
  }

  def updateItemList(checkCardState: CheckCardData, itemId: ItemId, content: Option[String], isChecked: Boolean = false) = {
    val currentItem = checkCardState.items.filter(_.itemId == itemId)
    LiveState(CheckCardData(checkCardState.checkCardId, checkCardState.createdAt, checkCardState.title,
      checkCardState.items.filter(_.itemId != itemId)
        :+ ItemData(currentItem.head.itemId, currentItem.head.createdAt, content, isChecked)))
  }
}

