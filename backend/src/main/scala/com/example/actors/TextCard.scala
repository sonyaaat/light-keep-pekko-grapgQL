package com.example.actors

import com.example.lib.JsonSerializable
import org.apache.pekko.actor.typed.scaladsl.{ ActorContext, Behaviors }
import org.apache.pekko.actor.typed.{ ActorRef, ActorSystem, Behavior }
import org.apache.pekko.cluster.sharding.typed.scaladsl.{ ClusterSharding, Entity, EntityRef, EntityTypeKey }
import org.apache.pekko.http.scaladsl.model.DateTime
import org.apache.pekko.pattern.StatusReply
import org.apache.pekko.persistence.typed.scaladsl.{ Effect, EventSourcedBehavior, ReplyEffect }
import org.apache.pekko.persistence.typed.{ PersistenceId, RecoveryCompleted }
import org.apache.pekko.Done

import java.time.LocalDateTime

object TextCardTags {
  val Tag = "text-card"
}

object TextCard {

  case class TextCardData(
    textCardId: String,
    createdAt: LocalDateTime,
    title: Option[String] = None,
    content: Option[String] = None
  )

  type TextCardId = String

  // commands-messages

  trait Command[F] extends JsonSerializable {
    def replyTo: ActorRef[StatusReply[F]]
  }

  case class Create(replyTo: ActorRef[StatusReply[TextCardData]]) extends Command[TextCardData]

  case class Delete(replyTo: ActorRef[StatusReply[TextCardId]]) extends Command[TextCardId]

  case class UpdateTitle(replyTo: ActorRef[StatusReply[TextCardData]], title: String) extends Command[TextCardData]

  case class UpdateContent(replyTo: ActorRef[StatusReply[TextCardData]], content: String) extends Command[TextCardData]

  case class GetOne(replyTo: ActorRef[StatusReply[TextCardData]]) extends Command[TextCardData]

  // events-persist to cassandra
  trait Event extends JsonSerializable

  case class Created(id: String) extends Event

  case class TitleUpdated(id: String, title: String) extends Event

  case class ContentUpdated(id: String, content: String) extends Event

  case class Deleted(id: String) extends Event

  // states
  sealed trait State extends JsonSerializable {
    def textCard: TextCardData
  }

  case class BlankState(textCard: TextCardData = TextCardData("", LocalDateTime.now)) extends State

  case class CreatedState(textCard: TextCardData) extends State

  case class DeletedState(textCard: TextCardData = TextCardData("", LocalDateTime.now)) extends State

  val TypeKey = EntityTypeKey[Command[_]]("TextCard")

  def register(implicit system: ActorSystem[_]): Unit =
    ClusterSharding(system).init(Entity(TypeKey)(context => TextCard(context.entityId)))

  def getEntityRef(id: String)(implicit system: ActorSystem[_]): EntityRef[Command[_]] =
    ClusterSharding(system).entityRefFor(TypeKey, id)

  def apply(textCardId: String): Behavior[Command[_]] = Behaviors.setup { context =>
    val capturedTextCardId = textCardId
    EventSourcedBehavior[Command[_], Event, State](
      persistenceId = PersistenceId.ofUniqueId(textCardId),
      emptyState = BlankState(), // unused
      commandHandler = commandHandler(context, textCardId),
      eventHandler = eventHandler(context)
    )
      .withTagger(_ => Set(TextCardTags.Tag))
      .receiveSignal { case (state, RecoveryCompleted) =>
        println("RECOVERED ALL")
      }

  }

  // persist
  def illegalCommandHandler(
    command: Command[_],
    context: ActorContext[Command[_]],
    errorMessage: String
  ): ReplyEffect[Nothing, State] = {
    context.log.error(errorMessage)
    Effect.none.thenReply(command.replyTo)((_: State) => StatusReply.error(errorMessage))
  }

  def exeptionCommand(context: ActorContext[Command[_]]): Effect[Event, State] = {
    context.log.error("There is no such command")
    Effect.none
  }

  def commandHandler(
    context: ActorContext[Command[_]],
    textCardId: String
  )(state: State, command: Command[_]): Effect[Event, State] = {
    val errorMessage: String = s"Illegal command: $command for check card id: $textCardId"

    state match {
      case BlankState(_)          =>
        command match {
          case Create(replyTo)           =>
            println("created textcardId", textCardId)
            Effect
              .persist(Created(textCardId))
              .thenReply(replyTo)(newState => StatusReply.success(newState.textCard))
          case GetOne(replyTo)           =>
            illegalCommandHandler(command, context, errorMessage)
          case UpdateTitle(replyTo, _)   =>
            illegalCommandHandler(command, context, errorMessage)
          case UpdateContent(replyTo, _) =>
            illegalCommandHandler(command, context, errorMessage)
          case Delete(replyTo)           =>
            illegalCommandHandler(command, context, errorMessage)
          case _                         =>
            exeptionCommand(context)
        }
      case CreatedState(textCard) =>
        command match {
          case Create(replyTo)                 =>
            illegalCommandHandler(command, context, errorMessage)
          case UpdateTitle(replyTo, title)     =>
            Effect
              .persist(TitleUpdated(textCardId, title))
              .thenReply(replyTo)(newState => StatusReply.success(newState.textCard))
          case UpdateContent(replyTo, content) =>
            Effect
              .persist(ContentUpdated(textCardId, content))
              .thenReply(replyTo)(newState => StatusReply.success(newState.textCard))
          case Delete(replyTo)                 =>
            Effect
              .persist(Deleted(textCardId))
              .thenReply(replyTo)(newState => StatusReply.success(newState.textCard.textCardId))
          case GetOne(replyTo)                 =>
            Effect.none.thenReply(replyTo)((newState: State) => StatusReply.success(newState.textCard))
          case _                               =>
            exeptionCommand(context)
        }

      case DeletedState(_) =>
        command match {
          case UpdateContent(replyTo, content) =>
            illegalCommandHandler(command, context, errorMessage)
          case UpdateTitle(replyTo, title)     =>
            illegalCommandHandler(command, context, errorMessage)
          case Create(replyTo)                 =>
            illegalCommandHandler(command, context, errorMessage)
          case Delete(replyTo)                 =>
            illegalCommandHandler(command, context, errorMessage)
          case GetOne(replyTo)                 =>
            illegalCommandHandler(command, context, errorMessage)
          case _                               =>
            exeptionCommand(context)
        }

    }
  }

  // event handler=> update state
  def eventHandler(context: ActorContext[Command[_]])(state: State, event: Event): State =
    state match {
      case BlankState(_)       =>
        event match {
          case event @ Created(id) =>
            println(s"I recovered $event")
            CreatedState(TextCardData(id, LocalDateTime.now, None, None))

        }
      case CreatedState(card)  =>
        event match {
          case event @ TitleUpdated(id: String, title: String) =>
            val newState: TextCardData = TextCardData(id, card.createdAt, Option(title), card.content)
            println(s"I recovered $event with state $newState")
            CreatedState(newState)

          case event @ ContentUpdated(id, content) =>
            val newState: TextCardData = TextCardData(id, card.createdAt, card.title, Option(content))
            println(s"I recovered $event with state $newState")
            CreatedState(newState)

          case event @ Deleted(id) =>
            val newState: TextCardData = TextCardData(id, LocalDateTime.now)
            println(s"I recovered $event with state $newState")
            DeletedState(newState)

        }
      case DeletedState(state) =>
        DeletedState(state)

    }

}
