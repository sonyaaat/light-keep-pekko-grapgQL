package com.example.components
import com.raquo.laminar.api.L._

import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

import java.util.UUID
import CheckCard.commandObserver
import com.example.Main.route
import CheckCard.checkCardsVar
object Item {

  import CheckCardData._

  def renderItemElement(
    checkCardItemId: UUID,
    checkCardItem: CheckCardItem,
    checkCardItemSignal: Signal[CheckCardItem]
  ): HtmlElement = {
    val isEditingVar = Var(false)
    li(
      cls := "todo",
      cls <-- checkCardItemSignal.map(item => Map("completed" -> item.isChecked)),//isn`t working
//      cls := ("todo" -> true, "completed" -> checkCardItem.isChecked),
      onDblClick.filter(_ => !isEditingVar.now()).mapTo(true) --> isEditingVar.writer,
      children <-- isEditingVar.signal.map[List[HtmlElement]] {
        case true  =>
          List(
            input(
              cls   := "edit-item",
              typ   := "text",
              forId := checkCardItemId.toString,
              defaultValue <-- checkCardItemSignal.map { el =>
                el.content match {
                  case Some(content) => content
                  case None          => "Title"
                }
              },
              onEnterPress.mapToValue.compose(_.flatMap { content =>
                val requestBody = UpdateContentRequest(content)
                println(content)
                FetchStream
                  .put(
                    s"$route/check-cards/${checkCardItem.checkCardId}/items/$checkCardItemId",
                    _.body(requestBody.asJson.toString()),
                    _.headers("Content-Type" -> "application/json")
                  )
                  .map { response =>
                    val result = decode[CheckCardData.CheckCardItem](response)
                    result match {
                      case Right(item) =>
                        println("---------------BEFORE-----------")
                        println(checkCardsVar.now())
                        checkCardsVar.update { checkCards =>
                          checkCards.map { checkCard =>
                            if (checkCard.checkCardId == checkCardItem.checkCardId) {
                              val updatedItems = checkCard.items.map { item =>
                                if (item.itemId == checkCardItemId) {
                                  item.copy(content = Some(content))
                                } else {
                                  item
                                }
                              }
                              checkCard.copy(items = updatedItems)
                            } else {
                              checkCard
                            }
                          }
                        }
                      case Left(error) =>
                        println(s"Decoding failed: $error")
                    }
                  }
                  .mapTo(())
              }) --> { _ =>
                println("---------------Aftter-----------")
                println(checkCardsVar.now())
                isEditingVar.set(false)
              },
              onBlur.mapToValue.compose(_.flatMap { content =>
                val requestBody = UpdateContentRequest(content)
                println(content)
                FetchStream
                  .put(
                    s"$route/check-cards/${checkCardItem.checkCardId}/items/$checkCardItemId",
                    _.body(requestBody.asJson.toString()),
                    _.headers("Content-Type" -> "application/json")
                  )
                  .map { response =>
                    val result = decode[CheckCardData.CheckCardItem](response)
                    result match {
                      case Right(item) =>
                        println("---------------BEFORE-----------")
                        println(checkCardsVar.now())
                        checkCardsVar.update { checkCards =>
                          checkCards.map { checkCard =>
                            if (checkCard.checkCardId == checkCardItem.checkCardId) {
                              val updatedItems = checkCard.items.map { item =>
                                if (item.itemId == checkCardItemId) {
                                  item.copy(content =item.content)
                                } else {
                                  item
                                }
                              }
                              checkCard.copy(items = updatedItems)
                            } else {
                              checkCard
                            }
                          }
                        }
                      case Left(error) =>
                        println(s"Decoding failed: $error")
                    }
                  }
                  .mapTo(())
              }) --> { _ =>
                println("---------------Aftter-----------")
                println(checkCardsVar.now())
                isEditingVar.set(false)
              }
            )
          )
        case false =>
          List(
            input(
              typ    := "checkbox",
              idAttr := checkCardItemId.toString,
              checked <-- checkCardItemSignal.map(_.isChecked),
              onInput.mapToChecked.compose(_.flatMap { checked =>
                val action = if (checked) "check" else "uncheck"
                FetchStream
                  .put(
                    s"$route/check-cards/${checkCardItem.checkCardId}/items/$checkCardItemId/$action"
                  )
                  .map { response =>
                    val result = decode[CheckCardData.CheckCardItem](response)
                    result match {
                      case Right(item) =>
                        println("---------------BEFORE-----------")
                        println(checkCardsVar.now())
                        checkCardsVar.update { checkCards =>
                          checkCards.map { checkCard =>
                            if (checkCard.checkCardId == checkCardItem.checkCardId) {
                              val updatedItems = checkCard.items.map { item =>
                                if (item.itemId == checkCardItemId) {
                                  item.copy(isChecked = item.isChecked)
                                } else {
                                  item
                                }
                              }
                              checkCard.copy(items = updatedItems)
                            } else {
                              checkCard
                            }
                          }
                        }
                      case Left(error) =>
                        println(s"Decoding failed: $error")
                    }
                  }
                  .mapTo(())
              }) --> { _ =>
                println("---------------AFTER-----------")
                println(checkCardsVar.now()) }
            ),
            label(
              cls    := "",
              forId  := checkCardItemId.toString,
              child.text <-- checkCardItemSignal.map { el =>
                el.content match {
                  case Some(content) => content
                  case None          => "Title"
                }
              }
            ),
            button(

              cls("icon-btn remove-item-btn"),
              i(cls:="fa-solid fa-minus minus"),
              onClick.compose(_.flatMap { _ =>
                AjaxStream
                  .delete(
                    s"$route/check-cards/${checkCardItem.checkCardId}/items/$checkCardItemId"
                  )
                  .map { response =>
                    val result = decode[CheckCardData.DeleteItemResponse](response.responseText)
                    result match {
                      case Right(item) =>
                        println("SUCCESS", item)
                        checkCardsVar.update(list =>
                          list.map { checkCard =>
                            if (checkCard.checkCardId == checkCardItem.checkCardId) {
                              println("DELETE ITEM")
                              checkCard.copy(items = checkCard.items.filterNot(_.itemId != checkCardItemId))
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
            )
          )

      }
    )
  }
}
