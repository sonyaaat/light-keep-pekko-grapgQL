package com.example.components
import com.raquo.laminar.api.L._


object Button {
  val ebus = EventBus[String]

  def navElement(): HtmlElement = {
    val showDropVar = Var(false)

    navTag(
      div(
        cls := "wrapper",
        input(tpe := "radio"),
        input(tpe := "radio"),
        ul(
          cls     := "nav-links",
          li(
            a(href    := "#", cls := "desktop-item", i(cls := "fa-regular fa-square-plus custom-icon-size")),
            input(tpe := "checkbox", checked <-- showDropVar.signal),
//            label(forId := "showDrop", cls := "mobile-item", "create"),
            ul(
              cls     := "drop-menu",
              li(
                a(
                  href := "#",
                  "Create text-card",
                  onClick.compose(_.flatMap { _ =>
                    FetchStream.post(s"http://localhost:8081/text-cards")
                  }) --> ebus.writer
                )
              ),
              li(
                a(
                  href := "#",
                  "Create check-card",
                  onClick.compose(_.flatMap { _ =>
                    FetchStream.post(s"http://localhost:8081/check-cards")
                  }) --> ebus.writer
                )
              )
            )
          )
        )
      )
    )
  }
}
