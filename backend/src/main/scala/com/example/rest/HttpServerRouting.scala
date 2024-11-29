/*
    GET /text-cards
       Response:
         200 OK
         JSON all textCards

    POST /text-cards
      Payload: (id) as JSON
      Response:
        201 Created
        Payload (id)

    GET /text-cards/uuid
      Response:
        200 OK
        JSON textCard details

        404 Not found

    PUT /text-cards/uuid/title
      Payload: (title) as JSON
      Response:
        1)  200 OK
            Payload: new textCard details as JSON
        2)  404 Not found

    PUT /text-cards/uuid/content
        Payload: (content) as JSON
        Response:
          1)  200 OK
              Payload: new textCard details as JSON
          2)  404 Not found
    DELETE /text-cards/uuid
          Response:
            1)  200 OK

            2)  404 Not found
 */

package com.example.rest

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.http.scaladsl.server.{ Directives, Route }
import org.apache.pekko.http.cors.scaladsl.CorsDirectives._

object HttpServerRouting extends Directives {

  def route(implicit
    system: ActorSystem[_],
    textCardService: TextCardService,
    checkCardService: CheckCardService
  ): Route =
    cors() {
      concat(
        new TextCardsRouting().route,
        new CheckCardsRouting().route
      )
    }
}
