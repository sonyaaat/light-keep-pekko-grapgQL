package com.example.GraphQL

import caliban.{GraphQL, RootResolver, graphQL}
import com.example.repository.{CheckCardRecord, ItemRecord}
import caliban.schema.Schema.auto._
import caliban.schema.ArgBuilder.auto._

import java.util.UUID
import com.example.dto.CheckCard.CheckCardIntegrationEvents._
import zio.stream.ZStream
import zio.{Task, ZIO, ZLayer}
import com.example.dto.CheckCard.{CheckCardDTO, CheckCardItemDTO}
import com.example.actors.CheckCard.{CheckCardId, ItemId}

import java.time.LocalDateTime
import com.example.dto.CheckCard.Conversions._

trait CheckCardApi {
  def api: GraphQL[CheckCardApi]
}

object CheckCardApi {
  case class CheckCardArgs(checkCardId: UUID)

  case class ItemArgs(checkCardId: UUID, itemId: UUID)

  case class CheckCardUpdTitleArgs(checkCardId: UUID, title: String)

  case class ItemUpdContentArgs(checkCardId: UUID, content: String, itemId: UUID)

  case class ItemResponse(
                           itemId: UUID,
                           createdAt: LocalDateTime,
                           content: Option[String] = None,
                           isChecked: Boolean = false,
                           checkCardId: UUID
                         )

  case class CheckCardResponse(
                                checkCardId: UUID,
                                createdAt: LocalDateTime,
                                title: Option[String] = None,
                                items: Task[List[ItemResponse]]
                              )

  case class Queries(
                      checkCard: CheckCardArgs => Task[Option[CheckCardDTO]],
                      checkCards: () => Task[List[CheckCardDTO]]
                    )

  case class Mutations(
                        createCheckCard: () => Task[CheckCardDTO],
                        createCheckCardItem: CheckCardArgs => Task[CheckCardItemDTO],
                        updateCheckCardTitle: CheckCardUpdTitleArgs => Task[CheckCardDTO],
                        updateCheckCardItemContent: ItemUpdContentArgs => Task[CheckCardItemDTO],
                        checkCheckCardItem: ItemArgs => Task[CheckCardItemDTO],
                        unCheckCheckCardItem: ItemArgs => Task[CheckCardItemDTO],
                        deleteCheckCardItem: ItemArgs => Task[ItemId],
                        deleteCheckCard: CheckCardArgs => Task[CheckCardId]
                      )

  case class Subscriptions(
                            onCheckCardEvent: ZStream[Any, Nothing, Event]
                          )

  def makeApi(checkCardService: CheckCardService): GraphQL[CheckCardApi] = {
    def getCheckCardItems(checkCardId:UUID): Task[List[ItemRecord]] = checkCardService.getAllItemsByCheckCardId(checkCardId.toString)
    val checkCardQueries = Queries(
      checkCard = checkCardArgs => {
        val checkCardOption: Task[Option[CheckCardRecord]] = checkCardService.getOne(checkCardArgs.checkCardId.toString)
        val itemsInitialData: Task[List[ItemRecord]] = getCheckCardItems(checkCardArgs.checkCardId)
        val res = checkCardOptionDTOfromRecord(checkCardOption, itemsInitialData)
        ZIO.succeed(res).flatten
      },
      checkCards = () =>
        checkCardService.getAll.map(checkCards =>
          checkCards.map { checkCard =>
            val itemsInitialData: Task[List[ItemRecord]] = getCheckCardItems(checkCard.id)
            checkCardDTOfromRecord(checkCard, itemsInitialData)
          }
        )
    )

    val checkCardMutations = Mutations(
      createCheckCard = () => checkCardService.create()
      ,
      createCheckCardItem = checkCardArgs => checkCardService.appendItem(checkCardArgs.checkCardId.toString),

      updateCheckCardTitle =
        checkCardArgs => checkCardService.updateTitle(checkCardArgs.checkCardId.toString, checkCardArgs.title).map(checkCard=>checkCardDTOfromData(checkCard,getCheckCardItems(UUID.fromString(checkCard.checkCardId)))),

      updateCheckCardItemContent = itemArgs =>
        checkCardService.updateItemContent(itemArgs.checkCardId.toString, itemArgs.itemId.toString, itemArgs.content),

      checkCheckCardItem =
        itemArgs => checkCardService.checkItem(itemArgs.checkCardId.toString, itemArgs.itemId.toString),

      unCheckCheckCardItem =
        itemArgs => checkCardService.unCheckItem(itemArgs.checkCardId.toString, itemArgs.itemId.toString),

      deleteCheckCardItem =
        itemArgs => checkCardService.removeItem(itemArgs.checkCardId.toString, itemArgs.itemId.toString),

      deleteCheckCard = checkCardArgs => checkCardService.delete(checkCardArgs.checkCardId.toString)
    )

    val checkCardSubscriptions: Subscriptions = Subscriptions(
      onCheckCardEvent = checkCardService.checkCardEvent
    )

    graphQL(RootResolver(checkCardQueries, checkCardMutations, checkCardSubscriptions))

  }

  val layer: ZLayer[CheckCardService, Nothing, CheckCardApi] =
    ZLayer(
      ZIO.serviceWith[CheckCardService](checkCardService =>
        new CheckCardApi {
          override def api: GraphQL[CheckCardApi] = makeApi(checkCardService)
        }
      )
    )
}
