package shopping.cart;

import akka.actor.typed.ActorSystem;
import akka.cluster.sharding.typed.ShardedDaemonProcessSettings;
import akka.cluster.sharding.typed.javadsl.ShardedDaemonProcess;
import akka.persistence.jdbc.query.javadsl.JdbcReadJournal;
import akka.persistence.query.Offset;
import akka.projection.ProjectionBehavior;
import akka.projection.ProjectionId;
import akka.projection.eventsourced.EventEnvelope;
import akka.projection.eventsourced.javadsl.EventSourcedProvider;
import akka.projection.javadsl.ExactlyOnceProjection;
import akka.projection.javadsl.SourceProvider;
import akka.projection.jdbc.javadsl.JdbcProjection;
import org.springframework.orm.jpa.JpaTransactionManager;
import shopping.cart.event.ShoppingCartEvent;
import shopping.cart.repository.HibernateJdbcSession;
import shopping.cart.repository.ItemPopularityRepository;

import java.util.Optional;

public class ItemPopularityProjection {
  private ItemPopularityProjection(){}

  public static void init(
      ActorSystem<?> system,
      JpaTransactionManager transactionManager,
      ItemPopularityRepository repository
  ){

    final ShardedDaemonProcess process = ShardedDaemonProcess.get(system);
    process.init(
        ProjectionBehavior.Command.class,
        "ItemPopularityProjection",
        ShoppingCart.TAGS.size(),
        index-> ProjectionBehavior.create(createProjectionFor(system, transactionManager, repository, index)),
        ShardedDaemonProcessSettings.create(system),
        Optional.of(ProjectionBehavior.stopMessage())

    );

  }

  private static ExactlyOnceProjection<Offset, EventEnvelope<ShoppingCartEvent>> createProjectionFor(
      ActorSystem<?> system,
      JpaTransactionManager transactionManager,
      ItemPopularityRepository repository,
      int index) {

    final String tag = ShoppingCart.TAGS.get(index);
    final SourceProvider<Offset,  EventEnvelope<ShoppingCartEvent>> sourceProvider =
        EventSourcedProvider.eventsByTag(system, JdbcReadJournal.Identifier(), tag);

    return JdbcProjection.exactlyOnce(
        ProjectionId.of("ItemPopularityProjection", tag),
        sourceProvider,
        ()-> new HibernateJdbcSession(transactionManager),
        () -> new ItemPopularityProjectionHandler(tag, repository),
        system
    );
  }

}
