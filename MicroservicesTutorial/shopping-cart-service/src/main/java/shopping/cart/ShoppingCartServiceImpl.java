package shopping.cart;


import akka.actor.typed.ActorSystem;
import akka.actor.typed.DispatcherSelector;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.EntityRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shopping.cart.command.AddItem;
import shopping.cart.command.AdjustItemQuantity;
import shopping.cart.command.Checkout;
import shopping.cart.command.Get;
import shopping.cart.command.RemoveItem;
import shopping.cart.model.ItemPopularity;
import shopping.cart.model.Summary;
import shopping.cart.proto.AddItemRequest;
import shopping.cart.proto.AdjustItemQuantityRequest;
import shopping.cart.proto.Cart;
import shopping.cart.proto.CheckoutRequest;
import shopping.cart.proto.GetCartRequest;
import shopping.cart.proto.GetItemPopularityRequest;
import shopping.cart.proto.GetItemPopularityResponse;
import shopping.cart.proto.RemoveItemRequest;
import shopping.cart.proto.ShoppingCartService;
import shopping.cart.repository.ItemPopularityRepository;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

import static shopping.cart.GrpcUtils.convertError;


public class ShoppingCartServiceImpl implements ShoppingCartService {

  private final ClusterSharding sharding;
  private final Duration timeout;

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final ItemPopularityRepository repository;

  private final Executor blockingJdbcExecutor;

  public ShoppingCartServiceImpl(
      ActorSystem<?> system,
      ItemPopularityRepository repository) {
    sharding = ClusterSharding.get(system);
    timeout = system.settings().config().getDuration("shopping-cart-service.ask-timeout");
    blockingJdbcExecutor = getBlockingJdbcExecutor(system);
    this.repository = repository;
  }

  private Executor getBlockingJdbcExecutor(ActorSystem<?> system) {
    return system.dispatchers().lookup(DispatcherSelector.fromConfig("akka.projection.jdbc.blocking-jdbc-dispatcher"));
  }

  @Override
  public CompletionStage<Cart> addItem(AddItemRequest in) {

    logger.info("addItem {} to cart {}", in.getItemId(), in.getCartId());
    final EntityRef<ShoppingCartCommand> entityRef = getEntityRef(in.getCartId());
    final CompletionStage<Summary> reply = entityRef.askWithStatus(replyTo -> new AddItem(in.getItemId(), in.getQuantity(), replyTo), timeout);

    final CompletionStage<Cart> cart = reply.thenApply(Mapper::toProtoSummary);

    return convertError(cart);
  }

  @Override
  public CompletionStage<Cart> removeItem(RemoveItemRequest in) {
    logger.info("Performing RemoveItem  to cart {}", in.getCartId());
    final EntityRef<ShoppingCartCommand> entityRef = getEntityRef(in.getCartId());

    final CompletionStage<Summary> reply = entityRef.askWithStatus(replyTo -> new RemoveItem(in.getItemId(), replyTo), timeout);

    final CompletionStage<Cart> response = reply.thenApply(Mapper::toProtoSummary);

    return convertError(response);
  }

  private EntityRef<ShoppingCartCommand> getEntityRef(String entityId) {
    return sharding.entityRefFor(ShoppingCart.ENTITY_TYPE_KEY, entityId);
  }

  @Override
  public CompletionStage<Cart> checkout(CheckoutRequest in) {
    logger.info("checkout {} ", in.getCartId());

    final EntityRef<ShoppingCartCommand> entityRef = getEntityRef(in.getCartId());
    final CompletionStage<Summary> summary = entityRef.askWithStatus(Checkout::new, timeout);
    final CompletionStage<Cart> cart = summary.thenApply(Mapper::toProtoSummary);
    return convertError(cart);

  }

  @Override
  public CompletionStage<Cart> getCart(GetCartRequest in) {
    logger.info("getCart {}", in.getCartId());
    final EntityRef<ShoppingCartCommand> entityRef = getEntityRef(in.getCartId());
    final CompletionStage<Summary> get = entityRef.ask(Get::new, timeout);

    final CompletionStage<Cart> protoCart = GrpcUtils.handleNotFound(
        get,
        summary -> summary.items().isEmpty(),
        Mapper::toProtoSummary,
        String.format("Cart %s is empty", in.getCartId()));

    return convertError(protoCart);

  }

  @Override
  public CompletionStage<Cart> adjustItemQuantity(AdjustItemQuantityRequest in) {
    logger.info("Performing AdjustItemQuantity  to entity {}", in.getCartId());
    final EntityRef<ShoppingCartCommand> entityRef = getEntityRef(in.getCartId());

    final CompletionStage<Summary> reply = entityRef.askWithStatus(replyTo -> new AdjustItemQuantity(in.getItemId(), in.getQuantity(), replyTo), timeout);

    final CompletionStage<Cart> response = reply.thenApply(Mapper::toProtoSummary);

    return convertError(response);
  }

  @Override
  public CompletionStage<GetItemPopularityResponse> getItemPopularity(GetItemPopularityRequest in) {

    final CompletableFuture<Optional<ItemPopularity>> itemPopularityOptional = CompletableFuture.supplyAsync(() -> repository.findById(in.getItemId()), blockingJdbcExecutor);
    return itemPopularityOptional.thenApply(Mapper::toProtoItemPopularity);
  }


}