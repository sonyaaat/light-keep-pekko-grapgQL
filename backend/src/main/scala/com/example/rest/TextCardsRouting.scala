package com.example.rest

import com.example.StartRest.system.executionContext
import com.example.actors.TextCard
import com.example.repository.{TextCardRepository, TextCardRepositoryImpl, TextCardRecord => TextCardTable}
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.http.scaladsl.server.{Directives, Route}
import org.apache.pekko.util.Timeout
import scalikejdbc._

import io.circe.generic.auto._
import org.mdedetrich.pekko.http.support.CirceHttpSupport

import scala.concurrent.duration.DurationInt

object TextCardsAPI {
  case class CreateResponse(textCardId: String)
  case class UpdateTitleRequest(title: String)
  case class UpdateContentRequest(content: String)
}

class TextCardsRouting(implicit system: ActorSystem[_], textCardService: TextCardService)
    extends Directives with CirceHttpSupport {

  import TextCardsAPI._
  implicit val timeout: Timeout = 10.seconds

  //  private def createHttpResponse(payload: String, statusCode: StatusCode = StatusCodes.OK): HttpResponse =
  //    HttpResponse(
  //      status = statusCode,
  //      entity = HttpEntity.Strict(ContentTypes.`application/json`, ByteString(payload))
  //    )

  //  private def onCompleteHandle(res: Future[StatusReply[_]], status: StatusCode = StatusCodes.OK,resType:type): Route =
  //
  //    onComplete(res) { data =>
  //      data match {
  //        case util.Failure(exception) =>
  //          println("ex", exception)
  //          complete(createHttpResponse(exception.toString, StatusCodes.NotFound))
  //        case util.Success(value: TextCard.Data) =>
  //          println("val", value)
  //          complete(createHttpResponse(data.toString, status))
  //      }
  //
  //    }

  private def hello: Route =
    path("hello") {
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to Pekko HTTP</h1>"))
      }
    }

  //  private def getOne: Route =
  //    get { // /<uuid> or /?uuid=<uuid>
  //      (path(Segment) | parameter("uuid".as[String])) { textCardId =>
  //        onComplete(textCardService.getOne(textCardId)) { data =>
  //          data match {
  //            case util.Failure(exception) =>
  //              complete(StatusCodes.NotFound, exception.toString)
  //            case util.Success(value: TextCard.Data) =>
  //              complete(StatusCodes.OK, value)
  //          }
  //        }
  //      }
  //    }

  private def getOne: Route =
    get { // /<uuid> or /?uuid=<uuid>
      (path(Segment) | parameter("uuid".as[String])) { textCardId =>
        // ToDo
        // How to catch error if id can't be converted to UUID?
        onComplete(textCardService.getOne(textCardId)) {
          case util.Success(Some(value)) =>
            complete(StatusCodes.OK, value)
          case util.Success(None)        =>
            complete(StatusCodes.NotFound, s"TextCard with id $textCardId doesn't exist")
          case util.Failure(exception) =>
//            complete(StatusCodes.BadRequest, s"Bad request for TextCard $textCardId")
            val msg = s"Failed to get TextCard by id $textCardId, $exception"
            system.log.error(msg)
            failWith(new RuntimeException(msg))
        }
      }
    }

  //  get {
  //    pathEndOrSingleSlash {
  //      implicit val textCardRepo: TextCardRepository = new TextCardRepositoryImpl()
  //      onComplete(textCardRepo.getAll()) { list =>
  //        list match {
  //          case util.Success(list: List[TextCardTable]) => complete(StatusCodes.OK, list)
  //          case util.Failure(exception) => complete(exception)
  //        }
  //      }
  //      //        val members: List[TextCardRepository] = sql"select * from text_card".map(rs => TextCardRepository(rs)).list.apply()
  //    }
  //  }

  private def create: Route =
    post {
      pathEndOrSingleSlash {
        onComplete(textCardService.create) { textCard =>
          textCard match {
            case util.Failure(exception)            =>
              complete(StatusCodes.NotFound, exception.toString)
            case util.Success(value: TextCard.TextCardData) =>
              complete(StatusCodes.Created, value)
          }
        }
      }
    }

  private def updateTitle: Route =
    put {
      (pathPrefix(Segment) | parameter("uuid".as[String])) { textCardId =>
        path("title") {
          entity(as[UpdateTitleRequest]) { req =>
            onComplete(textCardService.updateTitle(textCardId, req.title)) { textCard =>
              textCard match {
                case util.Failure(exception)            =>
                  complete(StatusCodes.NotFound, exception.toString)
                case util.Success(value: TextCard.TextCardData) =>
                  complete(StatusCodes.OK, value)
              }
            }
          }
        }
      }
    }

  private def updateContent: Route =
    put {
      (pathPrefix(Segment) | parameter("uuid".as[String])) { textCardId =>
        path("content") {
          entity(as[UpdateContentRequest]) { req =>
            onComplete(textCardService.updateContent(textCardId, req.content)) { textCard =>
              textCard match {
                case util.Failure(exception)            =>
                  complete(StatusCodes.NotFound, exception.toString)
                case util.Success(value: TextCard.TextCardData) =>
                  complete(StatusCodes.OK, value)
              }
            }
          }
        }
      }
    }

  private def remove: Route =
    delete {
      (path(Segment) | parameter("uuid".as[String])) { textCardId =>
        onComplete(textCardService.delete(textCardId)) { data =>
          data match {
            case util.Failure(exception)                  =>
              complete(StatusCodes.NotFound, exception.toString)
            case util.Success(value: TextCard.TextCardId) =>
              complete(StatusCodes.OK, CreateResponse(value))
          }
        }
      }
    }

  implicit val session = AutoSession
  val t = TextCardTable.syntax("t")

  private def getAll: Route =
    get {
      pathEndOrSingleSlash {
        implicit val textCardRepo: TextCardRepository = new TextCardRepositoryImpl()
        onComplete(textCardRepo.getAll()) { list =>
          list match {
            case util.Success(list: List[TextCardTable]) =>
              list.length match {
                case 0 => complete(StatusCodes.NotFound, s"TextCard list is empty")
                case _ => complete(StatusCodes.OK, list)
              }
            case util.Failure(exception)                 => complete(exception)
          }
        }
        //        val members: List[TextCardRepository] = sql"select * from text_card".map(rs => TextCardRepository(rs)).list.apply()
      }
    }

  def route: Route =
    pathPrefix("text-cards") {
      concat(
        hello,
        getOne,
        getAll,
        create,
        updateTitle,
        updateContent,
        remove
      )
    }
}
