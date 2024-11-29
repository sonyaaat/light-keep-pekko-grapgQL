package com.example.rest

import com.example.actors.CheckCard
import com.example.rest.CheckCardsAPI.CreateResponse
import com.typesafe.config.ConfigFactory
import org.apache.pekko.actor
import org.apache.pekko.actor.testkit.typed.scaladsl.ActorTestKit
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import org.apache.pekko.http.scaladsl.testkit.ScalatestRouteTest
import org.apache.pekko.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import com.example.actors.CheckCard.ItemData
import io.circe.generic.auto._
import org.mdedetrich.pekko.http.support.CirceHttpSupport


class CheckCardRecordRoutingSpec extends AnyWordSpecLike with Matchers with ScalatestRouteTest with CirceHttpSupport {
  val testCheckCardId = "555e4567-e89b-12d3-a456-426655440111"
  val testItemCheckCardId = "777e4567-e89b-12d3-a456-426655440777"
  lazy val testKit = ActorTestKit(
    EventSourcedBehaviorTestKit.config.withFallback(ConfigFactory.load("application-test.conf"))
  )
  implicit val typedSystem: ActorSystem[_] = testKit.system

  override def createActorSystem(): actor.ActorSystem = testKit.system.classicSystem

  private val route = HttpServerRouting.route(typedSystem, new TextCardServiceMock, new CheckCardServiceMock)
  "The CheckCard service" should {
    "return a hello for GET /check-cards/hello request" in {
      Get("/check-cards/hello") ~> route ~> check {
        responseAs[String] shouldEqual "<h1>Say hello to CheckCards</h1>"
        status shouldEqual StatusCodes.OK
      }
    }
    "create check card" in {
      Post("/check-cards") ~> route ~> check {
        status shouldEqual StatusCodes.Created
        responseAs[CheckCard.CheckCardData] should matchPattern { case CheckCard.CheckCardData(`testCheckCardId`, _, _, _) => }
      }
    }
    "successfully get check-card by id" in {
      testKit.createTestProbe()
      Get(s"/check-cards/${testCheckCardId}") ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[CheckCard.CheckCardData] should matchPattern { case CheckCard.CheckCardData(`testCheckCardId`, _, _, _) => }
      }
    }
    "throw exeption while getting not existing id" in {
      testKit.createTestProbe()
      Get(s"/check-cards/${"555e4569-e89b-12d3-a456-426655440111"}") ~> route ~> check {
        status shouldEqual StatusCodes.NotFound
      }

    }
    "change title successfully" in {
      testKit.createTestProbe()
      val title = "title"
      Put(s"/check-cards/${testCheckCardId}/title", HttpEntity(ContentTypes.`application/json`, s"""{ "title": "$title"}""")) ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[CheckCard.CheckCardData] should matchPattern { case CheckCard.CheckCardData(`testCheckCardId`, _, Some(`title`), _) => }
      }
    }
    "throw exeption while changing title to check-card with not existing id" in {
      testKit.createTestProbe()
      val title = "title"
      Put(s"/check-cards/222/title", HttpEntity(ContentTypes.`application/json`, s"""{ "title": "$title"}""")) ~> route ~> check {
        status shouldEqual StatusCodes.NotFound
      }

    }

    "delete textcard successfully" in {
      testKit.createTestProbe()
      Delete(s"/check-cards/${testCheckCardId}") ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[CreateResponse] should matchPattern { case CreateResponse(`testCheckCardId`) => }
      }

    }

    "throw exeption while deleting text-card with not existing id" in {
      testKit.createTestProbe()
      Delete(s"/check-cards/222") ~> route ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
    "create check card item" in {
      Post(s"/check-cards/$testCheckCardId/items") ~> route ~> check {
        status shouldEqual StatusCodes.Created
        responseAs[CheckCard.ItemData] should matchPattern { case ItemData(`testItemCheckCardId`, _, None, false) => }
      }
    }
    "throw exeption while creating check card item  for chck-card with not existing id" in {
      Post(s"/check-cards/222/items") ~> route ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
    "change item title successfully" in {
      val content = "content"
      Put(s"/check-cards/$testCheckCardId/items/$testItemCheckCardId", HttpEntity(ContentTypes.`application/json`, s"""{ "content": "$content"}""")) ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[CheckCard.ItemData] should matchPattern { case ItemData(`testItemCheckCardId`, _, Some(`content`), false) => }
      }
    }
    "throw exeption while changing item title with  not existing id" in {
      val title = "title"
      Put(s"/check-cards/222/items/$testItemCheckCardId", HttpEntity(ContentTypes.`application/json`, s"""{ "content": "$title"}""")) ~> route ~> check {
        status shouldEqual StatusCodes.NotFound

      }
    }
    "check item successfully" in {
      Put(s"/check-cards/$testCheckCardId/items/$testItemCheckCardId/check") ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[CheckCard.ItemData] should matchPattern { case ItemData(`testItemCheckCardId`, _, _, true) => }
      }
    }
    "throw exeption while checking item with  not existing id" in {
      Put(s"/check-cards/222/items/$testItemCheckCardId/check") ~> route ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
    "uncheck item successfully" in {
      Put(s"/check-cards/$testCheckCardId/items/$testItemCheckCardId/uncheck") ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[CheckCard.ItemData] should matchPattern { case ItemData(`testItemCheckCardId`, _, _, false) => }
      }
    }
    "throw exeption while unchecking item with not existing id" in {
      Put(s"/check-cards/222/items/$testItemCheckCardId/uncheck") ~> route ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
    "delete item successfully" in {
      testKit.createTestProbe()
      Delete(s"/check-cards/$testCheckCardId/items/$testItemCheckCardId") ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[CreateResponse] should matchPattern { case CreateResponse(`testItemCheckCardId`) => }
      }

    }
    "throw exeption while deleting item with not existing id" in {
      testKit.createTestProbe()
      Delete(s"/check-cards/222/items/$testItemCheckCardId") ~> route ~> check {
        status shouldEqual StatusCodes.NotFound

      }

    }

  }
}
