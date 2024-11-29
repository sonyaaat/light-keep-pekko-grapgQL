package com.example.rest

import scala.concurrent.ExecutionContext.Implicits.global
import com.example.actors.CheckCard
import com.example.actors.CheckCard._
import com.example.repository.{
  CheckCardRecord ,
  CheckCardRepository,
  CheckCardRepositoryImpl,
  ItemRecord
}
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.http.scaladsl.server.{ Directives, Route }
import org.apache.pekko.util.Timeout
import scalikejdbc._
import io.circe.generic.auto._
import org.mdedetrich.pekko.http.support.CirceHttpSupport

import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.{ Failure, Success }

object CheckCardsAPI {
  case class CreateResponseCheckCard(checkCard: CheckCardData)

  case class CreateResponse(checkCardId: String)

  case class UpdateTitleRequest(title: String)

  case class UpdateContentRequest(content: String)

  case class CheckCardResponse(
    checkCardId: UUID,
    createdAt: LocalDateTime,
    title: Option[String] = None,
    items: List[ItemResponse] = List()
  )

  final case class ItemResponse(
    itemId: UUID,
    createdAt: LocalDateTime,
    content: Option[String] = None,
    isChecked: Boolean = false,
    checkCardId: UUID
  )
}

class CheckCardsRouting(implicit checkCardService: CheckCardService) extends Directives with CirceHttpSupport {

  import CheckCardsAPI._

  implicit val timeout: Timeout = 10.seconds

  private def onCompleteHandle(res: Future[_], status: StatusCode = StatusCodes.OK): Route =
    onComplete(res) { data =>
      data match {
        case util.Failure(exception)                =>
          println("ex", exception)
          complete(StatusCodes.NotFound, exception.toString)
        case util.Success(value: CheckCardData)              =>
          println("valSuccess", value)
          complete(status, value)
        case util.Success(value: CheckCardRecord)    =>
          println("valSuccess", value)
          complete(status, value)
        case util.Success(item: ItemData)               =>
          println("itemSuccess", item)
          complete(status, item)
        case util.Success(checkCardId: CheckCardId) =>
          println("checkCardId", checkCardId)
          complete(StatusCodes.OK, CreateResponse(checkCardId))
        case util.Success(v)                        =>
          v match {
            case Some(value: CheckCardData) =>
              println("valSuccess", value)
              complete(status, value)
          }
      }
    }

  private def hello(implicit system: ActorSystem[_]): Route =
    path("hello") {
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to CheckCards</h1>"))
      }
    }

  private def create(implicit system: ActorSystem[_]): Route =
    post {
      pathEndOrSingleSlash {
        //        val checkCardId: String = UUID.randomUUID().toString
        //        val checkCard: EntityRef[CheckCard.Command[_]] = CheckCard.getEntityRef(checkCardId)
        //        val res: Future[Data] = checkCard.askWithStatus(ref => CheckCard.Create(ref))
        onCompleteHandle(checkCardService.create, StatusCodes.Created)
      }
    }

  private def updateTitle(implicit system: ActorSystem[_]): Route =
    put {
      (pathPrefix(Segment) | parameter("uuid".as[String])) { uuid =>
        path("title") {
          entity(as[UpdateTitleRequest]) { req =>
            println("title", req.title)

            onCompleteHandle(checkCardService.updateTitle(uuid, req.title),StatusCodes.OK)
          }
        }
      }
    }

  private def remove(implicit system: ActorSystem[_]): Route =
    delete {
      (path(Segment) | parameter("uuid".as[String])) { uuid =>
        onCompleteHandle(checkCardService.delete(uuid))
      }
    }

  private def appendItem(implicit system: ActorSystem[_]): Route =
    post {
      (path(Segment / "items") | parameter("uuid".as[String])) { uuid =>
        val itemFuture: Future[ItemData] = checkCardService.appendItem(uuid)
        onComplete(itemFuture) {
          case Failure(exception)     =>
            val msg = s"Failed to add item: $exception"
            system.log.error(msg)
//            failWith(new RuntimeException(msg))
            complete(StatusCodes.NotFound, exception.toString)
          case Success(itemRes: ItemData) =>
            complete(
              StatusCodes.Created,
              ItemResponse(
                UUID.fromString(itemRes.itemId),
                itemRes.createdAt,
                itemRes.content,
                itemRes.isChecked,
                UUID.fromString(uuid)
              )
            )
        }
      }
    }

  private def updateItemContent(implicit system: ActorSystem[_]): Route =
    put {
      (pathPrefix(Segment / "items") | parameter("uuid".as[String])) { checkCardUuid =>
        pathPrefix(Segment) { itemUuid =>
          entity(as[UpdateContentRequest]) { req =>
            println("content", req.content)
            val itemFuture: Future[ItemData] = checkCardService.updateItemContent(checkCardUuid, itemUuid, req.content)
            onComplete(itemFuture) {
              case Failure(exception)     =>
                val msg = s"Failed to edit item content: $exception"
                system.log.error(msg)
//                failWith(new RuntimeException(msg))
                complete(StatusCodes.NotFound, exception.toString)
              case Success(itemRes: ItemData) =>
                complete(
                  StatusCodes.OK,
                  ItemResponse(
                    UUID.fromString(itemRes.itemId),
                    itemRes.createdAt,
                    itemRes.content,
                    itemRes.isChecked,
                    UUID.fromString(checkCardUuid)
                  )
                )
            }
          }
        }
      }
    }

  private def removeItem(implicit system: ActorSystem[_]): Route =
    delete {
      (pathPrefix(Segment / "items") | parameter("uuid".as[String])) { checkCardUuid =>
        pathPrefix(Segment) { itemUuid =>
          onCompleteHandle(checkCardService.removeItem(checkCardUuid, itemUuid))
        }
      }
    }

  private def checkItem(implicit system: ActorSystem[_]): Route =
    put {
      (pathPrefix(Segment / "items") | parameter("uuid".as[String])) { checkCardUuid =>
        pathPrefix(Segment / "check") { itemUuid =>
          val itemFuture: Future[ItemData] = checkCardService.checkItem(checkCardUuid, itemUuid)
          onComplete(itemFuture) {
            case Failure(exception) =>
              val msg = s"Failed to add item: $exception"
              system.log.error(msg)
//              failWith(new RuntimeException(msg))
              complete(StatusCodes.NotFound, exception.toString)
            case Success(itemRes: ItemData) =>
              complete(
                StatusCodes.OK,
                ItemResponse(
                  UUID.fromString(itemRes.itemId),
                  itemRes.createdAt,
                  itemRes.content,
                  itemRes.isChecked,
                  UUID.fromString(checkCardUuid)
                )
              )
          }
        }
      }
    }

  private def unCheckItem(implicit system: ActorSystem[_]): Route =
    put {
      (pathPrefix(Segment / "items") | parameter("uuid".as[String])) { checkCardUuid =>
        pathPrefix(Segment / "uncheck") { itemUuid =>
          val itemFuture: Future[ItemData] = checkCardService.unCheckItem(checkCardUuid, itemUuid)
          onComplete(itemFuture) {
            case Failure(exception) =>
              val msg = s"Failed to add item: $exception"
              system.log.error(msg)
//              failWith(new RuntimeException(msg))
              complete(StatusCodes.NotFound, exception.toString)
            case Success(itemRes: ItemData) =>
              complete(
                StatusCodes.OK,
                ItemResponse(
                  UUID.fromString(itemRes.itemId),
                  itemRes.createdAt,
                  itemRes.content,
                  itemRes.isChecked,
                  UUID.fromString(checkCardUuid)
                )
              )
          }
        }
      }
    }

  private def getAll(implicit system: ActorSystem[_]): Route =
    get {
      pathEndOrSingleSlash {
        implicit val checkCardRepo: CheckCardRepository = new CheckCardRepositoryImpl()
        val futureCheckCards: Future[List[CheckCardRecord]] = checkCardRepo.getAll()
        onComplete(futureCheckCards) {
          case Failure(exception) =>
            val msg = s"Failed to get CheckCards: $exception"
            system.log.error(msg)
//            failWith(new RuntimeException(msg))
            complete(StatusCodes.NotFound, exception.toString)
          case Success(checkCardList) =>
            if (checkCardList.isEmpty) {
              complete(StatusCodes.OK, checkCardList)
            } else {
              val itemFutures = checkCardList.map { checkCard =>
                checkCardRepo.getAllItemsByCheckCardId(checkCard.id)
              }
              val allItemsFuture: Future[List[List[ItemRecord]]] = Future.sequence(itemFutures)

              onComplete(allItemsFuture) {
                case Failure(exception) =>
                  val msg = s"Failed to get Items for CheckCards: $exception"
                  system.log.error(msg)
//                  failWith(new RuntimeException(msg))
                  complete(StatusCodes.NotFound, exception.toString)

                case Success(checkCardItemsLists) =>
                  val checkCardWithItems = checkCardList.zip(checkCardItemsLists).map { case (checkCard, itemsList) =>
                    val responseCheckCardItem: List[ItemResponse] = itemsList.map(
                      (
                        item =>
                          ItemResponse(item.id, item.created_at, item.content, item.is_checked, item.check_card_id)
                      )
                    )
                    CheckCardResponse(checkCard.id, checkCard.created_at, checkCard.title, responseCheckCardItem)
                  }
                  complete(StatusCodes.OK, checkCardWithItems)
              }
            }
        }
        //        val members: List[TextCardRepository] = sql"select * from text_card".map(rs => TextCardRepository(rs)).list.apply()
      }
    }

  implicit val session = AutoSession

  private def getOne(implicit system: ActorSystem[_]): Route =
    get { // /<uuid> or /?uuid=<uuid>
      (path(Segment) | parameter("uuid".as[String])) { checkCardId =>
        val futureCheckCard: Future[Option[CheckCardRecord]] = checkCardService.getOne(checkCardId)
        val futureItems: Future[List[ItemRecord]] = checkCardService.getAllItemsByCheckCardId(checkCardId)
        println(futureCheckCard,futureItems)
        val combinedFuture: Future[(Option[CheckCardRecord], List[ItemRecord])] = for {
          result1 <- futureCheckCard
          result2 <- futureItems
        } yield (result1, result2)
        println(combinedFuture)
        onComplete(combinedFuture) {
//          ItemResponse(checkCardItem.id,checkCardItem.created_at,checkCardItem.content,checkCardItem.is_checked,checkCardItem.check_card_id)
          case util.Success((Some(checkCard), checkCardItems: List[ItemRecord])) =>
            val responseCheckCardItems: List[ItemResponse] = checkCardItems.map(
              (item => ItemResponse(item.id, item.created_at, item.content, item.is_checked, item.check_card_id))
            )
            complete(
              StatusCodes.OK,
              CheckCardResponse(checkCard.id, checkCard.created_at, checkCard.title, responseCheckCardItems)
            )
          case util.Failure(exception)                                          =>
            val msg = s"Failed to get CheckCard by id $checkCardId, $exception"
            system.log.error(msg)
//            failWith(new RuntimeException(msg))
            complete(StatusCodes.NotFound, exception.toString)
          case util.Success((None, _))                                          =>
            complete(StatusCodes.NotFound, s"CheckCard with id $checkCardId doesn't exist")
        }
      }
    }

  private def getAllItems(implicit system: ActorSystem[_]): Route =
    get { // /<uuid> or /?uuid=<uuid>
      (parameter("uuid".as[String]) | pathPrefix(Segment / "items")) { chechCardId =>
        implicit val checkCardRepo: CheckCardRepository = new CheckCardRepositoryImpl()
        onComplete(checkCardRepo.getAllItemsByCheckCardId(UUID.fromString(chechCardId))) {
          case util.Failure(exception)             =>
            val msg = s"Failed to get Items in CheckCard by id $chechCardId, $exception"
            system.log.error(msg)
//            failWith(new RuntimeException(msg))
            complete(StatusCodes.NotFound, exception.toString)
          case util.Success(list: List[ItemRecord]) =>
            list.length match {
              case 0 => complete(StatusCodes.NotFound, s"Items for CheckCard with id $chechCardId donʼt exist")
              case _ => complete(StatusCodes.OK, list)
            }
        }
      }
    }

  def route(implicit system: ActorSystem[_]): Route =
    pathPrefix("check-cards") {
      concat(
        hello,
        create,
        getOne,
        updateTitle,
        remove,
        appendItem,
        checkItem,
        unCheckItem,
        updateItemContent,
        removeItem,
        getAll,
        getAllItems
      )

    }
}
