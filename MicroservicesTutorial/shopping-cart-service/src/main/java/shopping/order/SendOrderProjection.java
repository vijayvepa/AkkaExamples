package shopping.order;

import akka.actor.typed.ActorSystem;
import akka.cluster.sharding.typed.ShardedDaemonProcessSettings;
import akka.cluster.sharding.typed.javadsl.ShardedDaemonProcess;
import akka.persistence.jdbc.query.javadsl.JdbcReadJournal;
import akka.persistence.query.Offset;
import akka.projection.ProjectionBehavior;
import akka.projection.ProjectionId;
import akka.projection.eventsourced.EventEnvelope;
import akka.projection.eventsourced.javadsl.EventSourcedProvider;
import akka.projection.javadsl.AtLeastOnceProjection;
import akka.projection.javadsl.SourceProvider;
import akka.projection.jdbc.javadsl.JdbcProjection;
import common.JpaSession;
import org.springframework.orm.jpa.JpaTransactionManager;
import shopping.cart.ShoppingCart;
import shopping.cart.ShoppingCartEvent;
import shopping.order.proto.ShoppingOrderService;

import java.util.Optional;

public class SendOrderProjection {

  private SendOrderProjection() {}

  public static void init(
      ActorSystem<?> system,
      JpaTransactionManager transactionManager,
      ShoppingOrderService orderService) {
    ShardedDaemonProcess.get(system)
        .init(
            ProjectionBehavior.Command.class,
            "SendOrderProjection",
            ShoppingCart.TAGS.size(),
            index ->
                ProjectionBehavior.create(
                    createProjectionsFor(system, transactionManager, orderService, index)),
            ShardedDaemonProcessSettings.create(system),
            Optional.of(ProjectionBehavior.stopMessage()));
  }

  private static AtLeastOnceProjection<Offset, EventEnvelope<ShoppingCartEvent>>
  createProjectionsFor(
      ActorSystem<?> system,
      JpaTransactionManager transactionManager,
      ShoppingOrderService orderService,
      int index) {
    String tag = ShoppingCart.TAGS.get(index);
    SourceProvider<Offset, EventEnvelope<ShoppingCartEvent>> sourceProvider =
        EventSourcedProvider.eventsByTag(system, JdbcReadJournal.Identifier(), tag);

    return JdbcProjection.atLeastOnceAsync(
        ProjectionId.of("SendOrderProjection", tag),
        sourceProvider,
        () -> new JpaSession(transactionManager),
        () -> new ShoppingOrderGrpcProjectionHandler(system, orderService),
        system);
  }
}