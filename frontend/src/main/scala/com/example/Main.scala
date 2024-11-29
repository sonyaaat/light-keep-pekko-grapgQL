package com.example
import com.example.components.Button.navElement
import com.example.components.{ CheckCard, TextCardGrid }
import com.example.components.{ CheckCard }
import com.raquo.laminar.api.L._
import org.scalajs.dom

object Main {
  val route="http://localhost:8081"
  def main(args: Array[String]): Unit = {
    val app = div(
      navElement(),
      div(
        cls := "main-grid",
        CheckCard.renderAllCheckCards,
        TextCardGrid(),

      )
    )

//    val someStringVar: Var[String] = Var[String]("initial")
//    val someStringSignal: Signal[String] = Signal.fromValue("Foo")
//    val someStringEventStream = EventStream.fromSeq(Seq("Bar1", "Bar2", "Bar3"))
//    val someStringEventBus = EventBus[String]()
//
//    val playgroud = div(
//      "Hi!",
//      p(
//        border := "1px solid black",
//        "Var",
//        br(),
//        child.text <-- someStringVar.signal
//      ),
//      p(
//        border := "1px solid black",
//        "EventBus",
//        br(),
//        child.text <-- someStringEventBus.events
//      ),
//      div(
//        input(
//          onInput.mapToValue --> { next => someStringVar.set(next) },
//          onInput.mapToValue --> someStringEventBus.writer
//        )
//      )
//    )

    // `#root` here must match the `id` in index.html
    val containerNode = dom.document.querySelector("#root")
    render(containerNode, app)
//    render(containerNode, playgroud)
  }
}
