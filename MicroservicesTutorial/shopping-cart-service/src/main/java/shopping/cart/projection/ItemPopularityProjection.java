package shopping.cart.projection;

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
import akka.projection.jdbc.javadsl.JdbcHandler;
import akka.projection.jdbc.javadsl.JdbcProjection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.jpa.JpaTransactionManager;
import shopping.cart.ShoppingCart;
import shopping.cart.event.ItemAdded;
import shopping.cart.event.ShoppingCartEvent;
import shopping.cart.model.ItemPopularity;
import shopping.cart.repository.ItemPopularityRepository;

import java.util.Optional;

public class ItemPopularityProjection {
  private ItemPopularityProjection() {
  }

  public static void init(
      ActorSystem<?> system,
      JpaTransactionManager transactionManager,
      ItemPopularityRepository repository
  ) {

    final ShardedDaemonProcess process = ShardedDaemonProcess.get(system);
    process.init(
        ProjectionBehavior.Command.class,
        "ItemPopularityProjection",
        ShoppingCart.TAGS.size(),
        index -> ProjectionBehavior.create(createItemPopularityProjectionForIndex(system, transactionManager, repository, index)),
        ShardedDaemonProcessSettings.create(system),
        Optional.of(ProjectionBehavior.stopMessage())

    );

  }

  private static ExactlyOnceProjection<Offset, EventEnvelope<ShoppingCartEvent>> createItemPopularityProjectionForIndex(
      ActorSystem<?> system,
      JpaTransactionManager transactionManager,
      ItemPopularityRepository repository,
      int index) {

    final String tag = ShoppingCart.TAGS.get(index);
    final SourceProvider<Offset, EventEnvelope<ShoppingCartEvent>> sourceProvider =
        EventSourcedProvider.eventsByTag(system, JdbcReadJournal.Identifier(), tag);

    return JdbcProjection.exactlyOnce(
        ProjectionId.of("ItemPopularityProjection", tag),
        sourceProvider,
        () -> new JpaSession(transactionManager),
        () -> new ItemPopularityProjectionHandler(tag, repository),
        system
    );
  }

  static class ItemPopularityProjectionHandler extends JdbcHandler<EventEnvelope<ShoppingCartEvent>, JpaSession> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final String tag;
    private final ItemPopularityRepository repository;

    public ItemPopularityProjectionHandler(
        String tag,
        ItemPopularityRepository repository) {
      this.tag = tag;
      this.repository = repository;
    }

    private ItemPopularity findOrNew(String itemId) {
      return repository.findById(itemId).orElseGet(() -> new ItemPopularity(itemId, 0L, 0L));
    }

    @Override
    public void process(
        JpaSession session,
        EventEnvelope<ShoppingCartEvent> eventEnvelope) {

      final ShoppingCartEvent event = eventEnvelope.event();


      Optional<ItemPopularity> updatedItemPopularity = getUpdatedItemPopularity(event);
      updatedItemPopularity.ifPresent(repository::save);
    }

    private Optional<ItemPopularity> getUpdatedItemPopularity(ShoppingCartEvent event) {

      if (event instanceof ItemAdded someItemAdded) {
        final ItemPopularity existingItemPopularity = findOrNew(someItemAdded.itemId());

        final ItemPopularity updatedItemPopularity = existingItemPopularity.changeCount(someItemAdded.quantity());
        logger.info("ItemPopularityProjectionHandler({}) item patched for '{}' : [{}]", this.tag, someItemAdded.itemId(), updatedItemPopularity);
        return Optional.of(updatedItemPopularity);

      }
      return Optional.empty();
    }

  }

}
