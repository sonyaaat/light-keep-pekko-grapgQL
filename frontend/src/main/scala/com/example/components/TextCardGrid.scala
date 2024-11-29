package com.example.components

import com.example.components.TextCard.{ renderTextCard, TextCard }
import com.raquo.laminar.api.L._
import io.circe.generic.auto._
import io.circe.parser._

object TextCardGrid {

  def textCardGrid(textCards: Signal[List[HtmlElement]]): HtmlElement = div(
    cls := "card-grid",
    div(
      cls := "card-grid-inner",
      children <-- textCards
    )
  )

  val textCardListVar: Var[List[TextCard]] = Var(List())

  val textCardListSignal: Signal[List[TextCard]] = textCardListVar.signal

  def apply(): HtmlElement = div(
    FetchStream.get("http://localhost:8081/text-cards") --> { res =>
      decode[List[TextCard]](res) match {
        case Left(error)         =>
          println("Error while parsing json", error)
          List()
        case Right(textCardList) =>
          val sortedList: List[TextCard] = textCardList.sortBy(_.created_at).reverse
          sortedList.foreach(s => println(s))
          textCardListVar.set(sortedList)

      }
    },
    textCardGrid(textCardListSignal.split(_.id)(renderTextCard))
  )

}
