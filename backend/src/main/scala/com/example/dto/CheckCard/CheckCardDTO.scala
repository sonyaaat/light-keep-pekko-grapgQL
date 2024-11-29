package com.example.dto.CheckCard

import caliban.schema.Annotations.GQLName
import zio.Task


import java.time.LocalDateTime
import java.util.UUID

@GQLName("CheckCard")
case class CheckCardDTO(
                         checkCardId: UUID,
                         createdAt: LocalDateTime,
                         title: Option[String] = None,
                         items: Task[Seq[CheckCardItemDTO]]

                       )
@GQLName("CheckCardItem")
case class CheckCardItemDTO(
                     itemId: UUID,
                     createdAt: LocalDateTime,
                     content: Option[String] = None,
                     isChecked: Boolean = false,
//                     checkCardId: UUID
                   )
