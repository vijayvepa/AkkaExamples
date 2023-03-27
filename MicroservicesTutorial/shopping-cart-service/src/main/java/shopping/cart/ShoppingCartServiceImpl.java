package shopping.cart;


import akka.actor.typed.ActorSystem;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.EntityRef;
import akka.grpc.GrpcServiceException;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shopping.cart.command.AddItem;
import shopping.cart.model.Summary;
import shopping.cart.proto.AddItemRequest;
import shopping.cart.proto.Cart;
import shopping.cart.proto.Item;
import shopping.cart.proto.ShoppingCartService;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeoutException;

public class ShoppingCartServiceImpl implements ShoppingCartService {

  private final ClusterSharding sharding;
  private final Duration timeout;

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  public ShoppingCartServiceImpl(ActorSystem<?> system) {
    sharding = ClusterSharding.get(system);
    timeout = system.settings().config().getDuration("shopping-cart-service.ask-timeout");
  }

  @Override
  public CompletionStage<Cart> addItem(AddItemRequest in) {

    logger.info("addItem {} to cart {}", in.getItemId(), in.getCartId());
    final EntityRef<ShoppingCartCommand> entityRef = sharding.entityRefFor(ShoppingCart.ENTITY_TYPE_KEY, in.getCartId());
    final CompletionStage<Summary> reply = entityRef.askWithStatus(replyTo -> new AddItem(in.getItemId(), in.getQuantity(), replyTo), timeout);

    final CompletionStage<Cart> cart = reply.thenApply(ShoppingCartServiceImpl::toProtoCart);

    return convertError(cart);
  }

  private <T> CompletionStage<T> convertError(CompletionStage<T> response) {
    return response.exceptionally(ex-> {
      if(ex instanceof TimeoutException){
        throw new GrpcServiceException(Status.UNAVAILABLE.withDescription("Operation timed out"));
      }
      throw new GrpcServiceException(Status.INVALID_ARGUMENT.withDescription(ex.getMessage()));
    });
  }

  private static Cart toProtoCart(Summary summary) {
    final List<Item> protoItems = summary.items().entrySet().stream()
        .map(entry -> Item.newBuilder()
            .setItemId(entry.getKey())
            .setQuantity(entry.getValue())
            .build()).toList();

    return Cart.newBuilder().addAllItems(protoItems).build();
  }
}