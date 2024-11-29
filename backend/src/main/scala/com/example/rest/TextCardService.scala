package com.example.rest

import com.example.actors.TextCard
import com.example.actors.TextCard.TextCardId
import com.example.repository.{TextCardRepository, TextCardRepositoryImpl, TextCardRecord => TextCardTable}
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.util.Timeout

import java.util.UUID
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

trait TextCardService {
  def getOne(textCardId: TextCard.TextCardId): Future[Option[TextCardTable]]
  def create: Future[TextCard.TextCardData]
  def updateTitle(textCardId: TextCard.TextCardId, title: String): Future[TextCard.TextCardData]
  def updateContent(textCardId: TextCard.TextCardId, content: String): Future[TextCard.TextCardData]
  def delete(textCardId: TextCard.TextCardId): Future[TextCardId]
}





class TextCardServiceImpl(implicit system: ActorSystem[_]) extends TextCardService {
  implicit val timeout: Timeout = 10.seconds
  implicit val ec: ExecutionContext = system.executionContext

  override def getOne(textCardId: TextCardId): Future[Option[TextCardTable]] = {
    implicit val textCardRepo: TextCardRepository = new TextCardRepositoryImpl()
    textCardRepo.getOne(UUID.fromString(textCardId))
  }
//    TextCard.getEntityRef(textCardId).askWithStatus(TextCard.GetOne)

  override def create: Future[TextCard.TextCardData] = {
    val textCardId: TextCardId = UUID.randomUUID().toString
    TextCard.getEntityRef(textCardId).askWithStatus(TextCard.Create)
  }

  override def updateTitle(textCardId: TextCardId, title: String): Future[TextCard.TextCardData] =
    TextCard.getEntityRef(textCardId).askWithStatus(ref => TextCard.UpdateTitle(ref, title))

  override def updateContent(textCardId: TextCardId, content: String): Future[TextCard.TextCardData] =
    TextCard.getEntityRef(textCardId).askWithStatus(ref => TextCard.UpdateContent(ref, content))

  override def delete(textCardId: TextCardId): Future[TextCardId] =
    TextCard.getEntityRef(textCardId).askWithStatus(TextCard.Delete)
}
