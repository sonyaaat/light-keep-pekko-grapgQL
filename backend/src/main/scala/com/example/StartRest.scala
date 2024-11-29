package com.example

import com.example.actors.{CheckCard, TextCard}
import com.example.projections.{CheckCardProjection, TextCardProjection}
import com.example.repository.{CheckCardRepository, CheckCardRepositoryImpl, TextCardRepository, TextCardRepositoryImpl}
import com.example.rest.{CheckCardService, CheckCardServiceImpl, HttpServerRouting, TextCardService, TextCardServiceImpl}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorSystem, Behavior}
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.util.Timeout
import scalikejdbc.config._

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}


object StartRest extends App {
  val userGuardianBehavior: Behavior[Unit] = Behaviors.setup { context =>
    context.log.info("User guardian is starting")
    Behaviors.empty
  }

  DBs.setupAll()

  implicit val system: ActorSystem[_] = ActorSystem(userGuardianBehavior, "text-card")
  implicit val ec: ExecutionContextExecutor = system.executionContext

  implicit val textCardService: TextCardService = new TextCardServiceImpl
  implicit val checkCardService: CheckCardService = new CheckCardServiceImpl

  implicit val timeout: Timeout = 10.seconds
  private val listenAddress = "0.0.0.0"
  private val listenPort = 8081

  implicit val textCardRepo: TextCardRepository = new TextCardRepositoryImpl()
  implicit val checkCardRepo: CheckCardRepository = new CheckCardRepositoryImpl()

  Http()
    .newServerAt(listenAddress, listenPort)
    .bind(HttpServerRouting.route)
    .onComplete {
      case Success(_)         =>
        println(s"Server now online. REST API is available at https://$listenAddress:$listenPort")
      case Failure(exception) =>
        println(s"Failed to start server: $exception")
        system.terminate()
    }

  CheckCard.register
  TextCard.register
  TextCardProjection.init
  CheckCardProjection.init

}
