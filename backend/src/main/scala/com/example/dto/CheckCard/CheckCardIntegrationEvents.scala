package com.example.dto.CheckCard

import caliban.schema.Annotations.GQLName
import com.example.actors.CheckCard.{CheckCardData, ItemData, CheckCardId => CheckCardId, ItemId => ItemId}

object CheckCardIntegrationEvents {
  sealed trait Event
  @GQLName("CheckCardCreated")
  case class Created(checkCardData: CheckCardData) extends Event
  @GQLName("CheckCardTitleUpdated")
  case class TitleUpdated(checkCardId: CheckCardId, title: String) extends Event
  @GQLName("CheckCardDeleted")
  case class Deleted(checkCardId: CheckCardId) extends Event
  @GQLName("CheckCardItemCreated")
  case class ItemAppended(itemData: ItemData) extends Event
  @GQLName("CheckCardItemUpdated")
  case class ItemUpdated(itemId: ItemId, content: String) extends Event
  @GQLName("CheckCardItemDeleted")
  case class ItemDeleted(itemId: ItemId) extends Event
  @GQLName("CheckCardItemChecked")
  case class ItemChecked(itemId: ItemId) extends Event
  @GQLName("CheckCardItemUnchecked")
  case class ItemUnchecked(itemId: ItemId) extends Event

}
