package com.example.components

import com.example.components.Item.renderItemElement
import com.raquo.laminar.api.L._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.scalajs.dom.KeyCode
import com.example.Main.route

import java.time.LocalDateTime
import java.util.UUID
object CheckCardData {
  case class CheckCardItem(
    itemId: UUID,
    createdAt: LocalDateTime,
    content: Option[String] = None,
    isChecked: Boolean = false,
    checkCardId: UUID
  )

  case class CheckCard(
    checkCardId: UUID,
    createdAt: LocalDateTime,
    title: Option[String] = None,
    items: List[CheckCardItem] = List()
  )
  case class DeleteItemResponse(checkCardId: UUID)
  case class UpdateContentRequest(content:String)

  sealed trait Command

  case class CheckItem(checkCardId: UUID, itemId: UUID, isChecked: Boolean) extends Command

  case class UpdateTitle(checkCardId: UUID, title: String) extends Command
  case class UpdateItemContent(checkCardId: UUID, itemId: UUID, content: String) extends Command
  case class DeleteItem(checkCardId: UUID, itemId: UUID) extends Command
  case class DeleteCheckCard(checkCardId: UUID) extends Command
  val onEnterPress = onKeyPress.filter(_.keyCode == KeyCode.Enter)
}
object CheckCard {
  import CheckCardData._
  val checkCardsVar: Var[List[CheckCard]] = Var(initial = List())
  def renderAllCheckCards(): HtmlElement =
    div(
      cls := "card-grid",
      button(
        "ADD CHECKCARD",
        onClick.compose(_.flatMap { _ =>
          FetchStream
            .post(
              s"$route/check-cards"
            )
            .map { response: String =>
              val result = decode[CheckCardData.CheckCard](response)
              result match {
                case Right(checkCard) =>
                  println("SUCCESS", checkCard)
                  val newCheckCard =
                    CheckCardData.CheckCard(checkCard.checkCardId, checkCard.createdAt, checkCard.title, List())
                  checkCardsVar.update(list => newCheckCard :: list)
                case Left(error)      =>
                  println(s"Decoding failed: $error")
              }
              println("response", response)
            }
            .mapTo(())
        }) --> { _ => println(s"Added", checkCardsVar.now()) }
      ),
      div(cls := "card-grid-inner", children <-- checkCardsVar.signal.split(_.checkCardId)(renderCheckCardElement)),
      FetchStream.get("http://localhost:8081/check-cards") --> { response =>
        val result = decode[List[CheckCard]](response)
        result match {
          case Right(checkCardResponses: List[CheckCard]) => // Successfully decoded
            checkCardResponses.foreach { (el: CheckCard) =>
              println(el)
              val sortedCheckCardResponses: List[CheckCard] = // sorting from newest to last
                checkCardResponses.sorted(Ordering.by((checkCard: CheckCard) => checkCard.createdAt).reverse)
              checkCardsVar.set(sortedCheckCardResponses)
            }
          case Left(error)                                => // Handle decoding error
            println(s"Decoding failed: $error")
        }
      }
    )

  val commandObserver = Observer[Command] {
    case CheckItem(checkCardId: UUID, itemId: UUID, isChecked: Boolean) =>
      println(checkCardId, itemId, isChecked)
      val check: String = isChecked match {
        case true  =>
          println("CHECK")
          "check"
        case false =>
          println("UNCHECK")
          "uncheck"
      }
      FetchStream.put(s"http://localhost:8081/check-cards/$checkCardId/$itemId/$check") --> { response: String =>
        println("FETCH")
        val result = decode[CheckCard](response)
        result match {
          case Right(checkCardResponses) => // Successfully decoded
            println("SUCCESS", checkCardResponses)
          case Left(error)               => // Handle decoding error
            println(s"Decoding failed: $error")
        }
      }
    case UpdateTitle(checkCardId: UUID, title: String)                  =>
      println(s"UPDATE TITLE,$checkCardId,$title")
    case UpdateItemContent(checkCardId, itemId, content)                =>
      println("UPDATE CONTENT", checkCardId, itemId, content)
    case DeleteItem(checkCardId, itemId)                                =>
      println("DELETE ITEM", checkCardId, itemId)
    case DeleteCheckCard(checkCardId)                                   =>
      println("Delete CheckCard", checkCardId)
    case _                                                              => println("JJJ")
    // TODO
  }

  def renderCheckCardElement(
    checkCardId: UUID,
    checkCard: CheckCard,
    checkCardSignal: Signal[CheckCard]
  ): HtmlElement = {
    case class UpdateTitleRequest(title: String)
//    val itemsSignal = checkCardSignal.map(_.items)

    div(
      cls := "card card-mid",
      div(
        cls := "card-inner",
        div(
          cls       := "header-line",
          h6("Todo"),
          a(
            i(
              cls := "fa-solid fa-trash bin"
            ),
            onClick.compose(_.flatMap { _ =>
              AjaxStream
                .delete(
                  s"$route/check-cards/${checkCardId}"
                )
                .map { response =>
                  val result = decode[CheckCardData.DeleteItemResponse](response.responseText)
                  result match {
                    case Right(item) =>
                      println("SUCCESS", item)
                      checkCardsVar.update(
                        _.filterNot(_.checkCardId == checkCardId)
                      )
                    case Left(error) =>
                      println(s"Decoding failed: $error")
                  }
                }
                .mapTo(())
            }) --> { _ => println(s"Added", checkCardsVar.now()) }
          ),
          onClick.map(_ => DeleteCheckCard(checkCardId)) --> commandObserver
        ),
        input(
          cls := "title-input title-input--checkcard",
          typ       := "text",
          defaultValue <-- checkCardSignal.map { el =>
            el.title match {
              case Some(title) => title
              case None        => "Title"
            }

          },
          onEnterPress.mapToValue.compose(_.flatMap { title =>
            val requestBody = UpdateTitleRequest(title)
            println(title)
            FetchStream
              .put(
                s"$route/check-cards/${checkCardId}/title",
                _.body(requestBody.asJson.toString()),
                _.headers("Content-Type" -> "application/json")
              )
              .mapTo(())
          }) --> { _ => println("***") },
          onBlur.mapToValue.compose(_.flatMap { title =>
            val requestBody = UpdateTitleRequest(title)
            println(title)
            FetchStream
              .put(
                s"$route/check-cards/${checkCardId}/title",
                _.body(requestBody.asJson.toString()),
                _.headers("Content-Type" -> "application/json")
              )
              .mapTo(())
          }) --> { _ => println("***") }
        ),
        ul(
          cls       := "todo-list",
          if (checkCard.items.length <= 0) h3("No Items")
          else
            children <-- checkCardSignal.map(_.items).split(_.itemId)(renderItemElement)
        ),

        div(cls := "card-bottom",linkTag(cls := "card-link", href := "#", "See all"), button(
          cls := "icon-btn",
          i(
            cls := "fa-solid fa-circle-plus plus"
          ),
          onClick.compose(_.flatMap { _ =>
            FetchStream
              .post(
                s"$route/check-cards/${checkCardId}/items"
              )
              .map { response: String =>
                val result = decode[CheckCardItem](response)
                result match {
                  case Right(item) =>
                    println("SUCCESS", checkCardId, item.itemId)
                    println("---------------BEFORE-----------")
                    println(checkCardsVar.now())
                    val newItem =
                      CheckCardItem(item.itemId, item.createdAt, item.content, item.isChecked, item.checkCardId)
                    checkCardsVar.update(list =>
                      list.map { checkCard =>
                        if (checkCard.checkCardId == checkCardId) {
                          println("ADD ITEM")
                          if (checkCard.items.length == 0) {
                            println(1)
                            checkCard.copy(items = List(newItem))
                          } else {
                            println(2)
                            checkCard.copy(items = newItem :: checkCard.items)
                          }
                        } else {
                          println("Prev")
                          checkCard
                        }
                      }
                    )
                  case Left(error) =>
                    println(s"Decoding failed: $error")
                }
                println("response", response)
              }
              .mapTo(())
          }) --> { _ => println(s"Added", checkCardsVar.now()) }
        ))
      )
    )
  }
}
