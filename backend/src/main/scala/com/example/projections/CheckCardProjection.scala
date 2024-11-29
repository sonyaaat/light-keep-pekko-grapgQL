package com.example.projections

import com.example.actors.{CheckCard, CheckCardTags}
import com.example.repository.CheckCardRepository
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.cluster.sharding.typed.scaladsl.ShardedDaemonProcess
import org.apache.pekko.persistence.cassandra.query.scaladsl.CassandraReadJournal
import org.apache.pekko.persistence.query.Offset
import org.apache.pekko.projection.cassandra.scaladsl.CassandraProjection
import org.apache.pekko.projection.eventsourced.EventEnvelope
import org.apache.pekko.projection.eventsourced.scaladsl.EventSourcedProvider
import org.apache.pekko.projection.scaladsl.SourceProvider
import org.apache.pekko.projection.{ProjectionBehavior, ProjectionId}
import org.apache.pekko.stream.connectors.cassandra.scaladsl.CassandraSessionRegistry

object CheckCardProjection {
  def init(implicit
            system: ActorSystem[_],
            repo: CheckCardRepository
          ): Unit = {
    println("INIT CHECK CARD PROJECTION")
    implicit val ec = system.executionContext
    val session = CassandraSessionRegistry(system).sessionFor("pekko.projection.cassandra.session-config")
    val sourceProvider: SourceProvider[Offset, EventEnvelope[CheckCard.Event]] =
      EventSourcedProvider
        .eventsByTag[CheckCard.Event](
          system,
          readJournalPluginId = CassandraReadJournal.Identifier,
          tag = CheckCardTags.Tag)
    val projection = CassandraProjection.atLeastOnce(
      projectionId = ProjectionId("check-card", CheckCardTags.Tag),
      sourceProvider,
      handler = () => new CheckCardProjectionHandler(CheckCardTags.Tag, system, repo))

    ShardedDaemonProcess(system).init[ProjectionBehavior.Command](
      name = "check-card",
      numberOfInstances = 1,
      behaviorFactory = _ => ProjectionBehavior(projection),
      stopMessage = ProjectionBehavior.Stop)
  }
}
