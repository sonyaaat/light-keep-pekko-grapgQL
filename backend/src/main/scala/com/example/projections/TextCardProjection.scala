package com.example.projections

import org.apache.pekko
import org.apache.pekko.projection.eventsourced.EventEnvelope
import pekko.persistence.cassandra.query.scaladsl.CassandraReadJournal
import pekko.persistence.query.Offset
import pekko.projection.eventsourced.scaladsl.EventSourcedProvider
import pekko.projection.scaladsl.SourceProvider
import com.example.actors.TextCard
import com.example.actors.TextCardTags
import pekko.projection.{ ProjectionBehavior, ProjectionId }
import pekko.projection.cassandra.scaladsl.CassandraProjection
import pekko.stream.connectors.cassandra.scaladsl.CassandraSessionRegistry
import com.example.repository.{ TextCardRepository, TextCardRepositoryImpl }
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.cluster.sharding.typed.scaladsl.ShardedDaemonProcess

object TextCardProjection {

  def init(implicit
    system: ActorSystem[_],
    repo: TextCardRepository
  ): Unit = {
    println("INIT")
    implicit val ec = system.executionContext
    val session = CassandraSessionRegistry(system).sessionFor("pekko.projection.cassandra.session-config")
    val sourceProvider: SourceProvider[Offset, EventEnvelope[TextCard.Event]] =
      EventSourcedProvider
        .eventsByTag[TextCard.Event](
          system,
          readJournalPluginId = CassandraReadJournal.Identifier,
          tag = TextCardTags.Tag
        )
    val projection = CassandraProjection.atLeastOnce(
      projectionId = ProjectionId("text-card", TextCardTags.Tag),
      sourceProvider,
      handler = () => new TextCardProjectionHandler(TextCardTags.Tag, system, repo)
    )

    ShardedDaemonProcess(system).init[ProjectionBehavior.Command](
      name = "text-card",
      numberOfInstances = 1, // TextCardTags.Tag.size,
      behaviorFactory = (_) => ProjectionBehavior(projection),
      stopMessage = ProjectionBehavior.Stop
    )
  }
}

