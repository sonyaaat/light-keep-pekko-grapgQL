package com.example.dto.CheckCard

import com.example.GraphQL.CheckCardService
import com.example.actors.CheckCard.{CheckCardData, ItemData}
import com.example.repository.{CheckCardRecord, ItemRecord}
import zio.{Task, ZIO}

import java.util.UUID
import scala.language.implicitConversions

object Conversions {
  implicit def checkCardDTOfromData(d: CheckCardData, items: Task[List[ItemRecord]]): CheckCardDTO = {
    CheckCardDTO(checkCardId = UUID.fromString(d.checkCardId), createdAt = d.createdAt, title = d.title, items)
  }

  implicit def checkCardDTOfromData(d: CheckCardData): CheckCardDTO = {
    CheckCardDTO(checkCardId = UUID.fromString(d.checkCardId), createdAt = d.createdAt, title = d.title, ZIO.succeed(Seq.empty))
  }


  implicit def checkCardItemDTOfromData(d: ItemData): CheckCardItemDTO = {
    CheckCardItemDTO(UUID.fromString(d.itemId), createdAt = d.createdAt, content = d.content, isChecked = d.isChecked)
  }

  implicit def checkCardOptionDTOfromRecord(r: Task[Option[CheckCardRecord]], items: Task[List[ItemRecord]]): ZIO[Any, Throwable, Option[CheckCardDTO]] = {
    r
      .map(checkCardOption =>
        checkCardOption.map(checkCard => CheckCardDTO(checkCard.id, checkCard.created_at, checkCard.title, items))
      )
  }

  implicit def checkCardDTOfromRecord(r: CheckCardRecord, items: Task[List[ItemRecord]]): CheckCardDTO = {
    CheckCardDTO(r.id, r.created_at, r.title, items)
  }

  implicit def mapItemsToResponse(items: Task[List[ItemRecord]]): ZIO[Any, Throwable, List[CheckCardItemDTO]] = {
    items.map(elems => elems.map(item =>
      CheckCardItemDTO(item.id, item.created_at, item.content, item.is_checked)))
  }
}
