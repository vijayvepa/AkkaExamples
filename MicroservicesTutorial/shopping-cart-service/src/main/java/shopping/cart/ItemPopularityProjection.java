package shopping.cart;

import akka.actor.typed.ActorSystem;
import akka.cluster.sharding.typed.ShardedDaemonProcessSettings;
import akka.cluster.sharding.typed.javadsl.ShardedDaemonProcess;
import akka.japi.function.Function;
import akka.persistence.jdbc.query.javadsl.JdbcReadJournal;
import akka.persistence.query.Offset;
import akka.projection.ProjectionBehavior;
import akka.projection.ProjectionId;
import akka.projection.eventsourced.EventEnvelope;
import akka.projection.eventsourced.javadsl.EventSourcedProvider;
import akka.projection.javadsl.ExactlyOnceProjection;
import akka.projection.javadsl.SourceProvider;
import akka.projection.jdbc.JdbcSession;
import akka.projection.jdbc.javadsl.JdbcHandler;
import akka.projection.jdbc.javadsl.JdbcProjection;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import shopping.cart.event.ItemAdded;
import shopping.cart.event.ShoppingCartEvent;
import shopping.cart.model.ItemPopularity;
import shopping.cart.repository.ItemPopularityRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
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

  /**
   * Hibernate based implementation of Akka Projection JdbcSession. This class is required when
   * building a JdbcProjection. It provides the means for the projection to start a transaction
   * whenever a new event envelope is to be delivered to the user defined projection handler.
   *
   * <p>The JdbcProjection will use the transaction manager to initiate a transaction to commit the
   * envelope offset. Then used in combination with JdbcProjection.exactlyOnce method, the user
   * handler code and the offset store operation participates on the same transaction.
   */
  static class JpaSession extends DefaultTransactionDefinition implements JdbcSession {

    private final JpaTransactionManager transactionManager;
    private final TransactionStatus transactionStatus;

    public JpaSession(JpaTransactionManager transactionManager) {
      this.transactionManager = transactionManager;
      this.transactionStatus = transactionManager.getTransaction(this);
    }

    public EntityManager entityManager() {
      return EntityManagerFactoryUtils.getTransactionalEntityManager(
          Objects.requireNonNull(transactionManager.getEntityManagerFactory()));
    }

    @SuppressWarnings({
        "resource" //causes RuntimeException
    })
    @Override
    public <Result> Result withConnection(Function<Connection, Result> func) {
      EntityManager entityManager = entityManager();
      Session hibernateSession = ((Session) entityManager.getDelegate());
      return hibernateSession.doReturningWork(
          connection -> {
            try {
              return func.apply(connection);
            } catch (SQLException e) {
              throw e;
            } catch (Exception e) {
              throw new SQLException(e);
            }
          });
    }

    @SuppressWarnings({
        "resource" //causes RuntimeException
    })
    @Override
    public void commit() {
      if (entityManager().isOpen()) transactionManager.commit(transactionStatus);
    }

    @SuppressWarnings({
        "resource" //causes RuntimeException
    })
    @Override
    public void rollback() {
      if (entityManager().isOpen()) transactionManager.rollback(transactionStatus);
    }

    @Override
    public void close() {
      entityManager().close();
    }
  }

}
