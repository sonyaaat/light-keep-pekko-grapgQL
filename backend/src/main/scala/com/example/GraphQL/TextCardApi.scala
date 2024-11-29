package com.example.GraphQL

import caliban.{ graphQL, GraphQL, RootResolver }
import caliban.schema.Schema.auto._
import caliban.schema.ArgBuilder.auto._

import com.example.dto.TextCard.TextCardIntegrationEvents._
import com.example.dto.TextCard.{ TextCardDTO => TextCardDTO }
import com.example.actors.TextCard.TextCardId

import zio.{ Task, ZIO }
import zio.ZLayer
import zio.stream.ZStream
import java.util.UUID

trait TextCardApi {
  def api: GraphQL[TextCardApi]
}

object TextCardApi {
  case class TextCardArgs(textCardId: UUID)

  case class CheckCardArgs(checkCardId: UUID)

  case class UpdateTextCardTitleArgs(textCardId: UUID, title: String)

  case class UpdateTextCardContentArgs(textCardId: UUID, content: String)

  case class Queries(
    textCard: TextCardArgs => Task[Option[TextCardDTO]],
    textCards: () => Task[List[TextCardDTO]]
  )

  case class Mutations(
    updateTextCardTitle: UpdateTextCardTitleArgs => Task[TextCardDTO],
    createTextCard: () => Task[TextCardDTO],
    updateTextCardContent: UpdateTextCardContentArgs => Task[TextCardDTO],
    deleteTextCard: TextCardArgs => Task[TextCardId]
  )

  case class Subscriptions(
    onTextCardEvent: ZStream[Any, Nothing, Event]
  )

  def makeApi(textCardService: TextCardService): GraphQL[TextCardApi] = {

    val textCarQueries = Queries(
      textCard = textCardArgs => textCardService.getOne(textCardArgs.textCardId.toString),
      textCards = () => textCardService.getAll
    )

    val textCardMutations = Mutations(
      updateTextCardTitle =
        textCardArgs => textCardService.updateTitle(textCardArgs.textCardId.toString, textCardArgs.title),
      createTextCard = () => textCardService.create,
      updateTextCardContent =
        textCardArgs => textCardService.updateContent(textCardArgs.textCardId.toString, textCardArgs.content),
      deleteTextCard = textCardArgs => textCardService.delete(textCardArgs.textCardId.toString)
    )

    val textCardSubscriptions = Subscriptions(
      textCardService.onTextCardEvent
    )

    graphQL(RootResolver(textCarQueries, textCardMutations, textCardSubscriptions))
  }

  val layer: ZLayer[TextCardService, Nothing, TextCardApi] =
    ZLayer(
      ZIO.serviceWith[TextCardService](textCardService =>
        new TextCardApi {
          override val api: GraphQL[TextCardApi] = makeApi(textCardService)
        }
      )
    )
}
