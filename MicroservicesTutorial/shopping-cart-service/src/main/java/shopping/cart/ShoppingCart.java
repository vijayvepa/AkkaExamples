package shopping.cart;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.SupervisorStrategy;
import akka.actor.typed.javadsl.Behaviors;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.Entity;
import akka.cluster.sharding.typed.javadsl.EntityContext;
import akka.cluster.sharding.typed.javadsl.EntityTypeKey;
import akka.pattern.StatusReply;
import akka.persistence.typed.PersistenceId;
import akka.persistence.typed.javadsl.CommandHandlerWithReply;
import akka.persistence.typed.javadsl.CommandHandlerWithReplyBuilder;
import akka.persistence.typed.javadsl.CommandHandlerWithReplyBuilderByState;
import akka.persistence.typed.javadsl.EventHandler;
import akka.persistence.typed.javadsl.EventSourcedBehavior;
import akka.persistence.typed.javadsl.EventSourcedBehaviorWithEnforcedReplies;
import akka.persistence.typed.javadsl.ReplyEffect;
import shopping.cart.command.AddItem;
import shopping.cart.command.AdjustItemQuantity;
import shopping.cart.command.Checkout;
import shopping.cart.command.Get;
import shopping.cart.command.RemoveItem;
import shopping.cart.event.CheckedOut;
import shopping.cart.event.ItemAdded;
import shopping.cart.event.ItemQuantityAdjusted;
import shopping.cart.event.ItemRemoved;
import shopping.cart.event.ShoppingCartEvent;
import shopping.cart.model.ShoppingCartState;
import shopping.cart.model.Summary;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static akka.pattern.StatusReply.error;
import static akka.pattern.StatusReply.success;

public class ShoppingCart extends EventSourcedBehaviorWithEnforcedReplies<ShoppingCartCommand, ShoppingCartEvent, ShoppingCartState> {

  public static final List<String> TAGS = List.of("carts-0", "carts-1", "carts-2", "carts-3", "carts-4");

  public static final EntityTypeKey<ShoppingCartCommand> ENTITY_TYPE_KEY =
      EntityTypeKey.create(ShoppingCartCommand.class, "ShoppingCart");

  private final String cartId;
  private final String projectionTag;

  private ShoppingCart(
      String cartId,
      String projectionTag) {
    super(PersistenceId.of(ENTITY_TYPE_KEY.name(), cartId));
    this.projectionTag = projectionTag;
    SupervisorStrategy.restartWithBackoff(Duration.ofMillis(200), Duration.ofSeconds(5), 0.1);
    this.cartId = cartId;
  }

  public static void init(ActorSystem<?> system) {
    final ClusterSharding clusterSharding = ClusterSharding.get(system);
    clusterSharding.init(Entity.of(ENTITY_TYPE_KEY, entityContext -> ShoppingCart.create(entityContext.getEntityId(), getRandomProjectionTag(entityContext))));
  }

  private static String getRandomProjectionTag(EntityContext<ShoppingCartCommand> entityContext) {
    final int tagIndex = Math.abs(entityContext.getEntityId().hashCode() % TAGS.size());
    return TAGS.get(tagIndex);
  }

  public static Behavior<ShoppingCartCommand> create(
      String cartId,
      String projectionTag) {
    return Behaviors.setup(ctx -> EventSourcedBehavior.start(new ShoppingCart(cartId, projectionTag), ctx));
  }

  @Override
  public Set<String> tagsFor(ShoppingCartEvent shoppingCartEvent) {
    return Collections.singleton(projectionTag);
  }

  @Override
  public ShoppingCartState emptyState() {
    return new ShoppingCartState(new HashMap<>(), Optional.empty());
  }

  @Override
  public CommandHandlerWithReply<ShoppingCartCommand, ShoppingCartEvent, ShoppingCartState> commandHandler() {
    return openShoppingCart().orElse(checkedOutShoppingCart()).orElse(getShoppingCart()).build();
  }

  private CommandHandlerWithReplyBuilderByState<ShoppingCartCommand, ShoppingCartEvent, ShoppingCartState, ShoppingCartState> openShoppingCart() {
    final CommandHandlerWithReplyBuilder<ShoppingCartCommand, ShoppingCartEvent, ShoppingCartState> builder = newCommandHandlerWithReplyBuilder();
    return builder.forState(state -> !state.isCheckedOut())
        .onCommand(AddItem.class, this::onAddItem)
        .onCommand(Checkout.class, this::onCheckout)
        .onCommand(RemoveItem.class, this::onRemoveItem)
        .onCommand(AdjustItemQuantity.class, this::onAdjustItemQuantity);

  }

  private CommandHandlerWithReplyBuilderByState<ShoppingCartCommand, ShoppingCartEvent, ShoppingCartState, ShoppingCartState> checkedOutShoppingCart() {
    final CommandHandlerWithReplyBuilder<ShoppingCartCommand, ShoppingCartEvent, ShoppingCartState> builder = newCommandHandlerWithReplyBuilder();
    return builder.forState(ShoppingCartState::isCheckedOut)
        .onCommand(AddItem.class, (state, command) -> replyError(command.replyTo(), "Can't add items to checked out cart"))
        .onCommand(RemoveItem.class, (state, command) -> replyError(command.replyTo(), "Can't remove items from checked out cart"))
        .onCommand(AdjustItemQuantity.class, (state, command) -> replyError(command.replyTo(), "Can't adjust quantity on items from checked out cart"))

        .onCommand(Checkout.class, (state, command) -> replyError(command.replyTo(), "Already checked out"));

  }

  private CommandHandlerWithReplyBuilderByState<ShoppingCartCommand, ShoppingCartEvent, ShoppingCartState, ShoppingCartState> getShoppingCart() {
    return newCommandHandlerWithReplyBuilder().forAnyState().onCommand(Get.class, this::onGet);
  }

  private ReplyEffect<ShoppingCartEvent, ShoppingCartState> onAddItem(
      ShoppingCartState state,
      AddItem command) {

    if (state.hasItem(command.itemId())) {
      return Effect().reply(command.replyTo(), error("Item " + command.itemId() + " ' was already added to this shopping cart."));

    }

    if (command.quantity() <= 0) {
      return Effect().reply(command.replyTo(), error("Quantity must be > 0"));
    }

    return Effect().persist(new ItemAdded(cartId, command.itemId(), command.quantity()))
        .thenReply(command.replyTo(), updatedCart -> success(updatedCart.toSummary()));
  }

  private ReplyEffect<ShoppingCartEvent, ShoppingCartState> onCheckout(
      ShoppingCartState state,
      Checkout command) {


    if (state.isEmpty()) {
      return Effect().reply(command.replyTo(), error("Cannot checkout an empty shopping cart."));
    }

    return Effect().persist(new CheckedOut(cartId, Instant.now()))
        .thenReply(command.replyTo(), updatedCart -> success(updatedCart.toSummary()));
  }

  private ReplyEffect<ShoppingCartEvent, ShoppingCartState> replyError(
      ActorRef<StatusReply<Summary>> statusReplyActorRef,
      String error) {
    return Effect().reply(statusReplyActorRef, error(error));
  }

  private ReplyEffect<ShoppingCartEvent, ShoppingCartState> onGet(
      ShoppingCartState state,
      Get command) {

    return Effect().reply(command.replyTo(), state.toSummary());
  }

  @Override
  public EventHandler<ShoppingCartState, ShoppingCartEvent> eventHandler() {
    return newEventHandlerBuilder().forAnyState()
        .onEvent(ItemAdded.class, this::updateItem)
        .onEvent(CheckedOut.class, this::handleCheckedOut)
        .onEvent(ItemRemoved.class, this::handleItemRemoved)
        .onEvent(ItemQuantityAdjusted.class, this::handleItemQuantityAdjusted)
        .build();
  }

  private ShoppingCartState updateItem(
      ShoppingCartState state,
      ItemAdded event) {
    return state.updateItem(event.itemId(), event.quantity());
  }

  private ShoppingCartState handleCheckedOut(
      ShoppingCartState state,
      CheckedOut event) {
    return state.checkout();
  }

  private ReplyEffect<ShoppingCartEvent, ShoppingCartState> onRemoveItem(
      ShoppingCartState state,
      RemoveItem command) {


    if (!state.hasItem(command.itemId())) {
      return Effect().reply(command.replyTo(), error("Item not found: " + command.itemId()));
    }

    return Effect().persist(new ItemRemoved(cartId, command.itemId()))
        .thenReply(command.replyTo(), updatedItem -> success(state.toSummary()));
  }


  private ShoppingCartState handleItemRemoved(
      ShoppingCartState state,
      ItemRemoved event) {
    return state.removeItem(event.itemId());
  }

  private ReplyEffect<ShoppingCartEvent, ShoppingCartState> onAdjustItemQuantity(
      ShoppingCartState state,
      AdjustItemQuantity command) {


    if (!state.hasItem(command.itemId())) {
      return Effect().reply(command.replyTo(), error("Item not found: " + command.itemId()));
    }

    if (command.updatedQuantity() < 0) {
      return Effect().reply(command.replyTo(), error("Quantity {} must be > 0" + command.updatedQuantity()));
    }

    return Effect().persist(new ItemQuantityAdjusted(cartId, command.itemId(), command.updatedQuantity()))
        .thenReply(command.replyTo(), updatedItem -> success(updatedItem.toSummary()));
  }


  private ShoppingCartState handleItemQuantityAdjusted(
      ShoppingCartState state,
      ItemQuantityAdjusted event) {
    return state.updateItem(event.itemId(), event.updatedQuantity());
  }


}
