package shopping.order;

import akka.Done;
import akka.actor.typed.ActorSystem;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.EntityRef;
import akka.projection.eventsourced.EventEnvelope;
import akka.projection.javadsl.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static akka.Done.done;


public final class ShoppingOrderGrpcProjectionHandler extends Handler<EventEnvelope<ShoppingCartEvent>> {

  private final Logger log = LoggerFactory.getLogger(getClass());
  private final ClusterSharding sharding;
  private final Duration timeout;
  private final ShoppingOrderService theShoppingOrderService;

  public ShoppingOrderGrpcProjectionHandler(
      ActorSystem<?> system, ShoppingOrderService shoppingOrderService) {
    sharding = ClusterSharding.get(system);
    timeout = system.settings().config().getDuration("shopping-cart-service.ask-timeout");
    this.theShoppingOrderService = shoppingOrderService;
  }

  @Override
  public CompletionStage<Done> process(EventEnvelope<ShoppingCartEvent> envelope) {
    if (envelope.event() instanceof CheckedOut theCheckedOutEvent) {
      return sendOrder(theCheckedOutEvent);
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

          final List<Item> items = cart.items().entrySet().stream()
              .map(entry -> Item.newBuilder().setQuantity(entry.getValue()).setItemId(entry.getKey()).build()
              ).toList();


          log.info("Sending order of {} items for cart {}.", cart.items().size(), checkedOut.cartId());
          OrderRequest orderRequest =
              OrderRequest.newBuilder().addAllItems(items).setCartId(checkedOut.cartId()).build();
          return theShoppingOrderService.order(orderRequest).thenApply(response -> done());
        });
  }
}