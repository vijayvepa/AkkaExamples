package shopping.order;

import akka.Done;
import akka.actor.typed.ActorSystem;
import akka.cluster.sharding.typed.ShardedDaemonProcessSettings;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.EntityRef;
import akka.cluster.sharding.typed.javadsl.ShardedDaemonProcess;
import akka.persistence.jdbc.query.javadsl.JdbcReadJournal;
import akka.persistence.query.Offset;
import akka.projection.ProjectionBehavior;
import akka.projection.ProjectionId;
import akka.projection.eventsourced.EventEnvelope;
import akka.projection.eventsourced.javadsl.EventSourcedProvider;
import akka.projection.javadsl.AtLeastOnceProjection;
import akka.projection.javadsl.Handler;
import akka.projection.javadsl.SourceProvider;
import akka.projection.jdbc.javadsl.JdbcProjection;
import common.JpaSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.jpa.JpaTransactionManager;
import shopping.cart.ShoppingCart;
import shopping.cart.ShoppingCartCommand;
import shopping.cart.ShoppingCartEvent;
import shopping.cart.command.Get;
import shopping.cart.event.CheckedOut;
import shopping.cart.model.Summary;
import shopping.order.proto.Item;
import shopping.order.proto.OrderRequest;
import shopping.order.proto.ShoppingOrderService;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static akka.Done.done;

public class ShoppingOrderProjection {

  private ShoppingOrderProjection() {
  }

  public static void init(
      ActorSystem<?> system,
      JpaTransactionManager transactionManager,
      ShoppingOrderService shoppingOrderService) {
    ShardedDaemonProcess.get(system)
        .init(
            ProjectionBehavior.Command.class,
            "ShoppingOrderProjection",
            ShoppingCart.TAGS.size(),
            index ->
                ProjectionBehavior.create(
                    createProjectionsFor(system, transactionManager, shoppingOrderService, index)),
            ShardedDaemonProcessSettings.create(system),
            Optional.of(ProjectionBehavior.stopMessage()));
  }

  private static AtLeastOnceProjection<Offset, EventEnvelope<ShoppingCartEvent>>
  createProjectionsFor(
      ActorSystem<?> system,
      JpaTransactionManager transactionManager,
      ShoppingOrderService shoppingOrderService,
      int index) {
    String tag = ShoppingCart.TAGS.get(index);
    SourceProvider<Offset, EventEnvelope<ShoppingCartEvent>> sourceProvider =
        EventSourcedProvider.eventsByTag(system, JdbcReadJournal.Identifier(), tag);

    return JdbcProjection.atLeastOnceAsync(
        ProjectionId.of("ShoppingOrderProjection", tag),
        sourceProvider,
        () -> new JpaSession(transactionManager),
        () -> new ShoppingOrderProjectionHandler(system, shoppingOrderService),
        system);
  }

  public static final class ShoppingOrderProjectionHandler extends Handler<EventEnvelope<ShoppingCartEvent>> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ClusterSharding sharding;
    private final Duration timeout;
    private final ShoppingOrderService shoppingOrderService;

    public ShoppingOrderProjectionHandler(
        ActorSystem<?> system, ShoppingOrderService shoppingOrderService) {
      sharding = ClusterSharding.get(system);
      timeout = system.settings().config().getDuration("shopping-cart-service.ask-timeout");
      this.shoppingOrderService = shoppingOrderService;
    }

    @Override
    public CompletionStage<Done> process(EventEnvelope<ShoppingCartEvent> envelope) {
      if (envelope.event() instanceof CheckedOut checkedOut) {
        return sendOrder(checkedOut);
      } else {
        return CompletableFuture.completedFuture(done());
      }
    }

    private CompletionStage<Done> sendOrder(CheckedOut checkedOut) {
      EntityRef<ShoppingCartCommand> entityRef =
          sharding.entityRefFor(ShoppingCart.ENTITY_TYPE_KEY, checkedOut.cartId());
      CompletionStage<Summary> reply =
          entityRef.ask(Get::new, timeout);
      return reply.thenCompose(
          cart -> {

            final List<Item> items =
                cart.items().entrySet().stream().map(entry -> Item.newBuilder().setQuantity(entry.getValue()).setItemId(entry.getKey()).build()).toList();
            OrderRequest orderRequest =
                OrderRequest.newBuilder().setCartId(checkedOut.cartId()).addAllItems(items).build();
            return shoppingOrderService.order(orderRequest).thenApply(response -> done());
          });
    }
  }
}