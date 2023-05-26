package shopping.cart;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.pattern.StatusReply;
import akka.persistence.testkit.javadsl.EventSourcedBehaviorTestKit;
import com.typesafe.config.ConfigFactory;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import shopping.cart.command.AddItem;
import shopping.cart.command.AdjustItemQuantity;
import shopping.cart.command.Checkout;
import shopping.cart.command.Get;
import shopping.cart.command.RemoveItem;
import shopping.cart.event.CheckedOut;
import shopping.cart.event.ItemAdded;
import shopping.cart.model.Summary;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("RedundantTypeArguments")
public class ShoppingCartTest {

  private static final String CART_ID = "testCart";

  @ClassRule
  public static final TestKitJunitResource testKit =
      new TestKitJunitResource(
          ConfigFactory.parseString("""
              akka.actor.serialization-bindings {
                "common.CborSerializable" = jackson-cbor
              }""")
              .withFallback(EventSourcedBehaviorTestKit.config()));

  private final EventSourcedBehaviorTestKit<ShoppingCartCommand, ShoppingCartEvent, ShoppingCartState> eventSourcedTestKit =
      EventSourcedBehaviorTestKit.create(testKit.system(), ShoppingCart.create(CART_ID, "ProjectionTag"));

  @Before
  public void beforeEach(){
    eventSourcedTestKit.clear();
  }

  @Test
  public void addAnItemToCart(){
    final EventSourcedBehaviorTestKit.CommandResultWithReply<ShoppingCartCommand, ShoppingCartEvent, ShoppingCartState, StatusReply<Summary>> result =
        eventSourcedTestKit.runCommand(replyTo -> new AddItem("foo", 42, replyTo));

    assertTrue(result.reply().isSuccess());
    final Summary value = result.reply().getValue();

    assertEquals(1, value.items().size());
    assertEquals(42, value.items().get("foo").intValue());
    assertEquals(new ItemAdded(CART_ID, "foo", 42), result.event());
  }

  @Test
  public void rejectAlreadyAddedItem(){
    {
      final EventSourcedBehaviorTestKit.CommandResultWithReply<ShoppingCartCommand, ShoppingCartEvent, ShoppingCartState, StatusReply<Summary>> result =
          eventSourcedTestKit.runCommand(replyTo -> new AddItem("foo", 42, replyTo));
      assertTrue(result.reply().isSuccess());
    }
    {
      final EventSourcedBehaviorTestKit.CommandResultWithReply<ShoppingCartCommand, ShoppingCartEvent, ShoppingCartState, StatusReply<Summary>> result =
          eventSourcedTestKit.runCommand(replyTo -> new AddItem("foo", 42, replyTo));
      assertTrue(result.reply().isError());
    }
  }

  @Test
  public void checkout(){
    final EventSourcedBehaviorTestKit.CommandResultWithReply<ShoppingCartCommand, ShoppingCartEvent, ShoppingCartState, StatusReply<Summary>> item =
        eventSourcedTestKit.runCommand(replyTo -> new AddItem("foo", 42, replyTo));

    assertTrue(item.reply().isSuccess());

    final EventSourcedBehaviorTestKit.CommandResultWithReply<ShoppingCartCommand, ShoppingCartEvent, ShoppingCartState, StatusReply<Summary>> checkout =
        eventSourcedTestKit.runCommand(Checkout::new);

    assertTrue(checkout.reply().isSuccess());
    assertTrue(checkout.event() instanceof CheckedOut);
    assertEquals(CART_ID, checkout.event().cartId());

    final EventSourcedBehaviorTestKit.CommandResultWithReply<ShoppingCartCommand, ShoppingCartEvent, ShoppingCartState, StatusReply<Summary>> item2 =
        eventSourcedTestKit.runCommand(replyTo -> new AddItem("foo2", 42, replyTo));

    assertTrue(item2.reply().isError());
  }

  @Test
  public void get() {
    final EventSourcedBehaviorTestKit.CommandResultWithReply<ShoppingCartCommand, ShoppingCartEvent, ShoppingCartState, StatusReply<Summary>> foo =
        eventSourcedTestKit.<StatusReply<Summary>>runCommand(replyTo -> new AddItem("foo", 42, replyTo));

    assertTrue(foo.reply().isSuccess());

    final EventSourcedBehaviorTestKit.CommandResultWithReply<ShoppingCartCommand, ShoppingCartEvent, ShoppingCartState, Summary> get = eventSourcedTestKit.<Summary>runCommand(Get::new);

    assertFalse(get.reply().checkedOut());
    assertEquals(1, get.reply().items().size());
    assertEquals(42, get.reply().items().get("foo").intValue());
  }

  @Test
  public void remove(){
    final EventSourcedBehaviorTestKit.CommandResultWithReply<ShoppingCartCommand, ShoppingCartEvent, ShoppingCartState, StatusReply<Summary>> add =
        eventSourcedTestKit.<StatusReply<Summary>>runCommand(replyTo -> new AddItem("foo", 43, replyTo));

    assertTrue(add.reply().isSuccess());
    assertEquals(1, add.reply().getValue().items().size());
    assertEquals(43, add.reply().getValue().items().get("foo").intValue());

    final EventSourcedBehaviorTestKit.CommandResultWithReply<ShoppingCartCommand, ShoppingCartEvent, ShoppingCartState, StatusReply<Summary>> remove =
        eventSourcedTestKit.<StatusReply<Summary>>runCommand(replyTo -> new RemoveItem("foo", replyTo));

    assertTrue(remove.reply().isSuccess());
    assertEquals(0, remove.reply().getValue().items().size());

    final EventSourcedBehaviorTestKit.CommandResultWithReply<ShoppingCartCommand, ShoppingCartEvent, ShoppingCartState, StatusReply<Summary>> remove2 =
        eventSourcedTestKit.<StatusReply<Summary>>runCommand(replyTo -> new RemoveItem("foo", replyTo));

    assertTrue(remove2.reply().isError());
    assertEquals("Item not found: foo", remove2.reply().getError().getMessage());
  }

  @Test
  public void adjustItemQuantity(){
    final EventSourcedBehaviorTestKit.CommandResultWithReply<ShoppingCartCommand, ShoppingCartEvent, ShoppingCartState, StatusReply<Summary>> add =
        eventSourcedTestKit.<StatusReply<Summary>>runCommand(replyTo -> new AddItem("foo", 44, replyTo));

    assertTrue(add.reply().isSuccess());
    assertEquals(1, add.reply().getValue().items().size());
    assertEquals(44, add.reply().getValue().items().get("foo").intValue());

    final EventSourcedBehaviorTestKit.CommandResultWithReply<ShoppingCartCommand, ShoppingCartEvent, ShoppingCartState, StatusReply<Summary>> update =
        eventSourcedTestKit.<StatusReply<Summary>>runCommand(replyTo -> new AdjustItemQuantity("foo", 40, replyTo));

    assertTrue(update.reply().isSuccess());
    assertEquals(1, update.reply().getValue().items().size());
    assertEquals(40, update.reply().getValue().items().get("foo").intValue());

    final EventSourcedBehaviorTestKit.CommandResultWithReply<ShoppingCartCommand, ShoppingCartEvent, ShoppingCartState, StatusReply<Summary>> remove =
        eventSourcedTestKit.<StatusReply<Summary>>runCommand(replyTo -> new RemoveItem("foo", replyTo));
    assertTrue(remove.reply().isSuccess());
    assertEquals(0, remove.reply().getValue().items().size());

    final EventSourcedBehaviorTestKit.CommandResultWithReply<ShoppingCartCommand, ShoppingCartEvent, ShoppingCartState, StatusReply<Summary>> update2 =
        eventSourcedTestKit.<StatusReply<Summary>>runCommand(replyTo -> new AdjustItemQuantity("foo", 20, replyTo));

    assertTrue(update2.reply().isError());
    assertEquals("Item not found: foo", update2.reply().getError().getMessage());

  }
}