package shopping.cart;

import akka.Done;
import akka.actor.CoordinatedShutdown;
import akka.actor.typed.ActorSystem;
import akka.cluster.sharding.typed.ShardedDaemonProcessSettings;
import akka.cluster.sharding.typed.javadsl.ShardedDaemonProcess;
import akka.japi.function.Function;
import akka.kafka.ProducerSettings;
import akka.kafka.javadsl.SendProducer;
import akka.persistence.jdbc.query.javadsl.JdbcReadJournal;
import akka.persistence.query.Offset;
import akka.projection.ProjectionBehavior;
import akka.projection.ProjectionId;
import akka.projection.eventsourced.EventEnvelope;
import akka.projection.eventsourced.javadsl.EventSourcedProvider;
import akka.projection.javadsl.AtLeastOnceProjection;
import akka.projection.javadsl.Handler;
import akka.projection.javadsl.SourceProvider;
import akka.projection.jdbc.JdbcSession;
import akka.projection.jdbc.javadsl.JdbcProjection;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import jakarta.persistence.EntityManager;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import shopping.cart.event.ItemAdded;
import shopping.cart.event.ShoppingCartEvent;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public final class ProduceEventsProjection {
  private ProduceEventsProjection() {
  }

  public static void init(ActorSystem<?> system, JpaTransactionManager transactionManager) {
    SendProducer<String, byte[]> sendProducer = createProducer(system);
    String topic = system.settings().config().getString("shopping-cart-service.kafka.topic");

    ShardedDaemonProcess.get(system).init(
        ProjectionBehavior.Command.class,
        "ProduceEventsProjection",
        ShoppingCart.TAGS.size(),
        index -> ProjectionBehavior.create(createProjectionFor(system, transactionManager, topic, sendProducer, index)),
        ShardedDaemonProcessSettings.create(system),
        Optional.of(ProjectionBehavior.stopMessage()));
  }

  private static SendProducer<String, byte[]> createProducer(ActorSystem<?> system) {
    ProducerSettings<String, byte[]> producerSettings = ProducerSettings.create(system, new StringSerializer(), new ByteArraySerializer());
    SendProducer<String, byte[]> sendProducer = new SendProducer<>(producerSettings, system);

    CoordinatedShutdown.get(system)
        .addTask(CoordinatedShutdown.PhaseActorSystemTerminate(), "close-sendProducer", sendProducer::close);
    return sendProducer;
  }

  private static AtLeastOnceProjection<Offset, EventEnvelope<ShoppingCartEvent>> createProjectionFor(
      ActorSystem<?> system,
      JpaTransactionManager transactionManager,
      String topic,
      SendProducer<String, byte[]> sendProducer,
      int index) {

    String tag = ShoppingCart.TAGS.get(index);
    SourceProvider<Offset, EventEnvelope<ShoppingCartEvent>> sourceProvider = EventSourcedProvider.eventsByTag(system, JdbcReadJournal.Identifier(), tag);

    return JdbcProjection.atLeastOnceAsync(
        ProjectionId.of("ProduceEventsProjection", tag),
        sourceProvider,
        () -> new JpaSession(transactionManager),
        () -> new ProduceEventsProjectionHandler(topic, sendProducer),
        system);
  }

  public static final class ProduceEventsProjectionHandler extends Handler<EventEnvelope<ShoppingCartEvent>> {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String topic;
    private final SendProducer<String, byte[]> sendProducer;

    public ProduceEventsProjectionHandler(
        String topic,
        SendProducer<String, byte[]> sendProducer) {
      this.topic = topic;
      this.sendProducer = sendProducer;
    }

    @Override
    public CompletionStage<Done> process(EventEnvelope<ShoppingCartEvent> envelope) {
      ShoppingCartEvent event = envelope.event();

      // using the cartId as the key and `DefaultPartitioner` will select partition based on the key
      // so that events for same cart always ends up in same partition
      String key = event.cartId();
      ProducerRecord<String, byte[]> producerRecord = new ProducerRecord<>(topic, key, serialize(event));
      return sendProducer
          .send(producerRecord)
          .thenApply(recordMetadata -> {
            logger.info("Published event [{}] to topic/partition {}/{}", event, topic, recordMetadata.partition());
            return Done.done();
          });
    }

    private static byte[] serialize(ShoppingCartEvent event) {
      final ByteString protoMessage;
      final String fullName;
      if (event instanceof ItemAdded someItemAdded) {
        protoMessage =
            shopping.cart.proto.ItemAdded.newBuilder()
                .setCartId(someItemAdded.cartId())
                .setItemId(someItemAdded.itemId())
                .setQuantity(someItemAdded.quantity())
                .build()
                .toByteString();
        fullName = shopping.cart.proto.ItemAdded.getDescriptor().getFullName();
      } else {
        throw new IllegalArgumentException("Unknown event type: " + event.getClass());
      }
      // pack in Any so that type information is included for deserialization
      return Any.newBuilder()
          .setValue(protoMessage)
          .setTypeUrl("shopping-cart-service/" + fullName)
          .build()
          .toByteArray();
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

