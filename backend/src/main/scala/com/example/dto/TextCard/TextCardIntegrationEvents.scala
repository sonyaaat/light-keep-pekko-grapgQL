package com.example.dto.TextCard

import caliban.schema.Annotations.GQLName

object TextCardIntegrationEvents {
  @GQLName("TextCardEvent")
  sealed trait Event
  @GQLName("TextCardCreated")
  case class Created(textCard: TextCardDTO) extends Event
  @GQLName("TextCardTitleUpdated")
  case class TitleUpdated(textCardId: String, title: String) extends Event
  @GQLName("TextCardContentUpdated")
  case class ContentUpdated(textCardId: String, content: String) extends Event
  @GQLName("TextCardDeleted")
  case class Deleted(textCardId: String) extends Event

}
