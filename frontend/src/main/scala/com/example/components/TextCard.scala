package com.example.components
import com.raquo.laminar.api.L._
import io.circe.generic.auto._
import org.scalajs.dom
import io.circe.syntax._

import java.time.LocalDateTime
import java.util.UUID

object TextCard {

  final case class TextCard(
    id: UUID,
    title: Option[String],
    content: Option[String],
    created_at: LocalDateTime
  )

  case class TitleBodyRequest(title: String)
  case class ContentBodyRequest(content: String)

  val ebus: EventBus[String] = new EventBus[String]
  val eStream: EventStream[String] = ebus.events
  val updatesObserver: Observer[String] = Observer[String](onNext = x => println(x))

  val onEnterPress: EventProcessor[dom.KeyboardEvent, dom.KeyboardEvent] =
    onKeyPress.filter(_.keyCode == dom.KeyCode.Enter)
  def renderTextCard(id: UUID, textCard: TextCard, textCardSignal: Signal[TextCard]): HtmlElement = div(
    cls := "card card-light",
    div(
      cls := "card-inner",
      div(
        cls := "header-line",
        h6("Text card"),
        a(
          i(
            cls := "fa-solid fa-trash bin"
          ),
          onClick.compose(_.flatMap { _ =>
            AjaxStream.delete(s"http://localhost:8081/text-cards/${id}").map { res =>
              res.responseText
              println(res.responseText)
            }

          }) --> { _ =>
            println("deleted text card with id")
          }
        )
      ),
      input(
        cls := "title-input",
        defaultValue <-- textCardSignal.map(t =>
          t.title match {
            case Some(title) => title
            case None        => "Title"
          }
        ),
        onEnterPress.mapToValue.compose(_.flatMap { newTitle =>
          FetchStream.put(
            s"http://localhost:8081/text-cards/${id}/title",
            _.body(TitleBodyRequest(newTitle).asJson.toString()),
            _.headers("Content-Type" -> "application/json")
          )

        }) --> ebus.writer,
        eStream --> updatesObserver
      ),
      textArea(
        cls := "content-input",
        defaultValue <-- textCardSignal.map(t =>
          t.content match {
            case Some(content) => content
            case None          => "Content"
          }
        ),
        onEnterPress.mapToValue.compose(_.flatMap { newContent =>
          FetchStream.put(
            s"http://localhost:8081/text-cards/${id}/content",
            _.body(ContentBodyRequest(newContent).asJson.toString()),
            _.headers("Content-Type" -> "application/json")
          )

        }) --> ebus.writer,
        eStream --> updatesObserver
      ),
      a(
        cls := "card-link",
        "Read more"
      )
    )
  )

}
