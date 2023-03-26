package shopping.cart;

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
import akka.persistence.typed.javadsl.EventHandler;
import akka.persistence.typed.javadsl.EventSourcedBehavior;
import akka.persistence.typed.javadsl.EventSourcedBehaviorWithEnforcedReplies;
import akka.persistence.typed.javadsl.ReplyEffect;
import shopping.cart.command.AddItem;
import shopping.cart.event.ItemAdded;
import shopping.cart.event.ShoppingCartEvent;
import shopping.cart.model.ShoppingCartState;

import java.time.Duration;

public class ShoppingCart extends EventSourcedBehaviorWithEnforcedReplies<ShoppingCartCommand, ShoppingCartEvent, ShoppingCartState> {

  static final EntityTypeKey<ShoppingCartCommand> ENTITY_TYPE_KEY =
      EntityTypeKey.create(ShoppingCartCommand.class, "ShoppingCart");

  private final  String cartId;

  public static void init(ActorSystem<?> system){
    final ClusterSharding clusterSharding = ClusterSharding.get(system);
    clusterSharding.init(Entity.of(ENTITY_TYPE_KEY, entityContext->ShoppingCart.create(entityContext.getEntityId())));
  }

  public static Behavior<ShoppingCartCommand> create(String cartId) {
    return Behaviors.setup(ctx-> EventSourcedBehavior.start(new ShoppingCart(cartId), ctx));
  }

  private ShoppingCart(String cartId){
    super(PersistenceId.of(ENTITY_TYPE_KEY.name(), cartId));
    SupervisorStrategy.restartWithBackoff(Duration.ofMillis(200), Duration.ofSeconds(5), 0.1);
    this.cartId = cartId;
  }


  @Override
  public ShoppingCartState emptyState() {
    return null;
  }

  @Override
  public CommandHandlerWithReply<ShoppingCartCommand, ShoppingCartEvent, ShoppingCartState> commandHandler() {
    final CommandHandlerWithReplyBuilder<ShoppingCartCommand, ShoppingCartEvent, ShoppingCartState> builder = newCommandHandlerWithReplyBuilder();
    builder.forAnyState().onCommand(AddItem.class, this::onAddItem);
    return builder.build();
  }

  private ReplyEffect<ShoppingCartEvent, ShoppingCartState> onAddItem(ShoppingCartState state, AddItem command) {

    if(state.hasItem(command.itemId())){
      return Effect().reply(command.replyTo(), StatusReply.error("Item " + command.itemId() + " ' was already added to this shopping cart."));

    }

    if(command.quantity() <= 0){
      return Effect().reply(command.replyTo(), StatusReply.error("Quantity must be > 0"));
    }

    return Effect().persist(new ItemAdded(cartId, command.itemId(), command.quantity()))
        .thenReply(command.replyTo(), updatedCart -> StatusReply.success(updatedCart.toSummary()));
  }

  @Override
  public EventHandler<ShoppingCartState, ShoppingCartEvent> eventHandler() {
    return newEventHandlerBuilder().forAnyState().onEvent(ItemAdded.class, this::updateItem).build();
  }

  private ShoppingCartState updateItem(ShoppingCartState state, ItemAdded event) {
    return state.updateItem(event.itemId(), event.quantity());
  }
}
