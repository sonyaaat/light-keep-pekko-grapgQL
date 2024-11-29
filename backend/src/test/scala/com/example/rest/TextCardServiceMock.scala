package com.example.rest

import com.example.actors.TextCard
import com.example.actors.TextCard.TextCardId
import com.example.repository.{TextCardRecord => TextCardTable}

import java.util.UUID
import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

class TextCardServiceMock extends TextCardService {
  val testTextCardId = "123e4567-e89b-12d3-a456-426655440000"
  val badTestTextCardId = "223e4567-e89b-12d3-a456-426655440000"

  import ExecutionContext.Implicits.global

  override def getOne(textCardId: TextCardId): Future[Option[TextCardTable]] = {
    Future {
      textCardId match {
        case `testTextCardId` => Option(TextCardTable(UUID.fromString(testTextCardId), Option("title"), Option("content"), LocalDateTime.now))
        case _ => None
      }
    }
  }


  override def create: Future[TextCard.TextCardData] = {
    Future {
      TextCard.TextCardData(testTextCardId, LocalDateTime.now, None, None)
    }
  }

  override def updateTitle(textCardId: TextCardId, title: TextCardId): Future[TextCard.TextCardData] = {
    Future {
      textCardId match {
        case `testTextCardId` => TextCard.TextCardData(testTextCardId, LocalDateTime.now, Option(title), None)
        case _ => throw new RuntimeException("Unexpected textCardId")
      }
    }
  }

  override def updateContent(textCardId: TextCardId, content: TextCardId): Future[TextCard.TextCardData] = {
    Future {
      textCardId match {
        case `testTextCardId` => TextCard.TextCardData(testTextCardId, LocalDateTime.now, None, Option(content))
        case _ => throw new RuntimeException("Unexpected textCardId")
      }
    }

  }

  override def delete(textCardId: TextCardId): Future[TextCardId] = {
    Future {
      textCardId match {
        case `testTextCardId` => textCardId
        case _ => throw new RuntimeException("Unexpected textCardId")
      }
    }
  }
}
