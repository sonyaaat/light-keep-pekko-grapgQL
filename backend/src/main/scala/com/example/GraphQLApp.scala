package com.example

import caliban._
import caliban.interop.tapir.{HttpInterpreter, WebSocketInterpreter}
import com.example.projections.{CheckCardProjection, TextCardProjection}
import com.example.GraphQL.{CheckCardApi, CheckCardService, TextCardApi, TextCardService}
import com.example.repository.{CheckCardRepository, CheckCardRepositoryImpl, TextCardRepository, TextCardRepositoryImpl}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorSystem, Behavior}
import org.apache.pekko.util.Timeout
import scalikejdbc.config._
import sttp.tapir.json.circe._
import zio._
import zio.http._
import zio.http.Header.{AccessControlAllowMethods, AccessControlAllowOrigin, Origin}
import zio.http.HttpAppMiddleware.cors
import zio.http.internal.middlewares.Cors.CorsConfig

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.{FiniteDuration, SECONDS}


class NotFoundError(message: String) extends CalibanError.ExecutionError(message)

object GraphQLApp extends ZIOAppDefault {
  val userGuardianBehavior: Behavior[Unit] = Behaviors.setup { context =>
    context.log.info("User guardian is starting")
    Behaviors.empty
  }

  DBs.setupAll()

  implicit val system: ActorSystem[_] = ActorSystem(userGuardianBehavior, "text-card")
  implicit val ec: ExecutionContextExecutor = system.executionContext

  implicit val timeout: Timeout = Timeout(FiniteDuration(10, SECONDS))

  implicit val checkCardRepo: CheckCardRepository = new CheckCardRepositoryImpl()

  implicit val textCardRepo: TextCardRepository = new TextCardRepositoryImpl()

  actors.TextCard.register
  TextCardProjection.init
  actors.CheckCard.register
  CheckCardProjection.init
   val config: CorsConfig =
    CorsConfig(
      allowedOrigin = _ => Some(AccessControlAllowOrigin.All),
      allowedMethods = AccessControlAllowMethods(Method.PUT, Method.DELETE,Method.GET,Method.POST),
    )

  val app: ZIO[CheckCardApi with TextCardApi with Server, Throwable, Unit] = for {
    textCardApi  <- ZIO.service[TextCardApi]
    checkCardApi <- ZIO.service[CheckCardApi]
    interpreter  <- (textCardApi.api |+| checkCardApi.api).interpreter // ZIO.serviceWithZIO[GraphQL[Any]](_.interpreter)
    _            <-
      Server
        .serve(
          Http
            .collectHttp[Request] {
              case _ -> Root / "api" / "graphql" => ZHttpAdapter.makeHttpService(HttpInterpreter(interpreter))
              case _ -> Root / "ws" / "graphql"  =>
                ZHttpAdapter.makeWebSocketService(WebSocketInterpreter(interpreter))
            }@@ cors(config)
        )
    _            <- Console.printLine("Server online at http://localhost:8088/api/graphql")
    _            <- Console.printLine("Press RETURN to stop...") *> Console.readLine
  } yield ()

  override def run: ZIO[Any, Throwable, Unit] =
    app.provide(
      TextCardService.make,
      TextCardApi.layer,
      CheckCardService.make,
      CheckCardApi.layer,
      ZLayer.succeed(
        Server.Config.default
          .port(8088)
          .withWebSocketConfig(ZHttpAdapter.defaultWebSocketConfig)
      ),
      Server.live
    )

}
