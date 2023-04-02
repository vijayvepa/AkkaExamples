package shopping.cart;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.SupervisorStrategy;
import akka.actor.typed.javadsl.Behaviors;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.Entity;
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
import shopping.cart.command.Checkout;
import shopping.cart.command.Get;
import shopping.cart.event.CheckedOut;
import shopping.cart.event.ItemAdded;
import shopping.cart.event.ShoppingCartEvent;
import shopping.cart.model.ShoppingCartState;
import shopping.cart.model.Summary;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Optional;

import static akka.pattern.StatusReply.error;
import static akka.pattern.StatusReply.success;

public class ShoppingCart extends EventSourcedBehaviorWithEnforcedReplies<ShoppingCartCommand, ShoppingCartEvent, ShoppingCartState> {

  static final EntityTypeKey<ShoppingCartCommand> ENTITY_TYPE_KEY =
      EntityTypeKey.create(ShoppingCartCommand.class, "ShoppingCart");

  private final String cartId;

  private ShoppingCart(String cartId) {
    super(PersistenceId.of(ENTITY_TYPE_KEY.name(), cartId));
    SupervisorStrategy.restartWithBackoff(Duration.ofMillis(200), Duration.ofSeconds(5), 0.1);
    this.cartId = cartId;
  }

  public static void init(ActorSystem<?> system) {
    final ClusterSharding clusterSharding = ClusterSharding.get(system);
    clusterSharding.init(Entity.of(ENTITY_TYPE_KEY, entityContext -> ShoppingCart.create(entityContext.getEntityId())));
  }

  public static Behavior<ShoppingCartCommand> create(String cartId) {
    return Behaviors.setup(ctx -> EventSourcedBehavior.start(new ShoppingCart(cartId), ctx));
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
        .onCommand(Checkout.class, this::onCheckout);

  }

  private CommandHandlerWithReplyBuilderByState<ShoppingCartCommand, ShoppingCartEvent, ShoppingCartState, ShoppingCartState> checkedOutShoppingCart() {
    final CommandHandlerWithReplyBuilder<ShoppingCartCommand, ShoppingCartEvent, ShoppingCartState> builder = newCommandHandlerWithReplyBuilder();
    return builder.forState(ShoppingCartState::isCheckedOut)
        .onCommand(AddItem.class, (state, command) -> replyError(command.replyTo(), "Can't add items to checked out cart"))
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
        .onEvent(CheckedOut.class, this::handleCheckedOut).build();
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


}
