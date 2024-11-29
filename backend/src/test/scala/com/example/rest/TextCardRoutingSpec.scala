package com.example.rest

import com.example.actors.TextCard
import com.example.rest.TextCardsAPI.CreateResponse
import com.example.repository.{ TextCardRecord => TextCardTable }
import com.typesafe.config.ConfigFactory
import org.apache.pekko.http.scaladsl.model.{ ContentTypes, HttpEntity }
import org.apache.pekko.actor.testkit.typed.scaladsl.ActorTestKit
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.http.scaladsl.model.{ DateTime, StatusCodes }
import org.apache.pekko.http.scaladsl.testkit.ScalatestRouteTest
import org.apache.pekko.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import org.apache.pekko.actor
import io.circe.generic.auto._
import org.mdedetrich.pekko.http.support.CirceHttpSupport
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID

class TextCardRoutingSpec extends AnyWordSpecLike with Matchers with ScalatestRouteTest with CirceHttpSupport {
  val testTextCardId = "123e4567-e89b-12d3-a456-426655440000"
  val badTestTextCardId = "223e4567-e89b-12d3-a456-426655440000"
  val testTextCardIdUUID: UUID = UUID.fromString(testTextCardId)
  val testCheckCardId = "555e4567-e89b-12d3-a456-426655440111"
  lazy val testKit: ActorTestKit = ActorTestKit(
    EventSourcedBehaviorTestKit.config.withFallback(ConfigFactory.load("application-test.conf"))
  )
  implicit val typedSystem: ActorSystem[_] = testKit.system

  override def createActorSystem(): actor.ActorSystem = testKit.system.classicSystem

  private val route = HttpServerRouting.route(typedSystem, new TextCardServiceMock, new CheckCardServiceMock)

  "The TextCard service" should {
//    "return a hello for GET /text-cards/hello request" in {
//      Get("/text-cards/hello") ~> route ~> check {
//        responseAs[String] shouldEqual <h1>Say hello to Pekko HTTP</h1>
//        status shouldEqual StatusCodes.OK
//      }
//    }

    "create text card" in {
      Post("/text-cards") ~> route ~> check {
        status shouldEqual StatusCodes.Created
        responseAs[TextCard.TextCardData] should matchPattern { case TextCard.TextCardData(`testTextCardId`, _, _, _) =>
        }
      }
    }

    "successfully get text-card by id" in {
      testKit.createTestProbe()
      Get(s"/text-cards/${testTextCardId}") ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[TextCardTable] should matchPattern { case TextCardTable(`testTextCardIdUUID`, _, _, _) => }
      }
    }

    "throw exception while getting not existing id" in {
      testKit.createTestProbe()
      Get(s"/text-cards/${badTestTextCardId}") ~> route ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }

//    "throw exception while getting id with not uuid type" in {
//      testKit.createTestProbe()
//      Get(s"/text-cards/222") ~> route ~> check {
//        status shouldEqual StatusCodes.NotFound
//      }
//    }

    "change title successfully" in {
      testKit.createTestProbe()
      val title = "title"
      Put(
        s"/text-cards/${testTextCardId}/title",
        HttpEntity(ContentTypes.`application/json`, s"""{ "title": "$title"}""")
      ) ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[TextCard.TextCardData] should matchPattern {
          case TextCard.TextCardData(`testTextCardId`, _, Some(`title`), _) =>
        }
      }
    }

    "throw exception while changing title to text-card with not existing id" in {
      testKit.createTestProbe()
      val title = "title"
      Put(
        s"/text-cards/${badTestTextCardId}/title",
        HttpEntity(ContentTypes.`application/json`, s"""{ "title": "$title"}""")
      ) ~> route ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }

//    "throw exception while changing title to text-card with not uuid type id" in {
//      testKit.createTestProbe()
//      val title = "title"
//      Put(s"/text-cards/222/title", HttpEntity(ContentTypes.`application/json`, s"""{ "title": "$title"}""")) ~> route ~> check {
//        status shouldEqual StatusCodes.BadRequest
//      }
//    }

    "change content successfully" in {
      testKit.createTestProbe()
      val content = "content"
      Put(
        s"/text-cards/${testTextCardId}/content",
        HttpEntity(ContentTypes.`application/json`, s"""{ "content": "${content}"}""")
      ) ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[TextCard.TextCardData] should matchPattern {
          case TextCard.TextCardData(`testTextCardId`, _, _, Some(`content`)) =>
        }
      }
    }

    "throw exception while changing content to text-card with not existing id" in {
      testKit.createTestProbe()
      val content = "content"
      Put(
        s"/text-cards/222/content",
        HttpEntity(ContentTypes.`application/json`, s"""{ "content": "$content"}""")
      ) ~> route ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }

    "delete textcard successfully" in {
      testKit.createTestProbe()
      Delete(s"/text-cards/${testTextCardId}") ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[CreateResponse] should matchPattern { case CreateResponse(`testTextCardId`) => }
      }

    }

    "throw exception while deleting text-card with not existing id" in {
      testKit.createTestProbe()
      Delete(s"/text-cards/222") ~> route ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }
}
