package shopping.cart;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.pattern.StatusReply;
import akka.persistence.testkit.javadsl.EventSourcedBehaviorTestKit;
import com.typesafe.config.ConfigFactory;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import shopping.cart.command.AddItem;
import shopping.cart.command.Checkout;
import shopping.cart.event.CheckedOut;
import shopping.cart.event.ItemAdded;
import shopping.cart.event.ShoppingCartEvent;
import shopping.cart.model.ShoppingCartState;
import shopping.cart.model.Summary;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ShoppingCartTest {

  private static final String CART_ID = "testCart";

  @ClassRule
  public static final TestKitJunitResource testKit =
      new TestKitJunitResource(
          ConfigFactory.parseString("""
              akka.actor.serialization-bindings {
                "shopping.cart.CborSerializable" = jackson-cbor
              }""")
              .withFallback(EventSourcedBehaviorTestKit.config()));

  private final EventSourcedBehaviorTestKit<ShoppingCartCommand, ShoppingCartEvent, ShoppingCartState> eventSourcedTestKit =
      EventSourcedBehaviorTestKit.create(testKit.system(), ShoppingCart.create(CART_ID));

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
    assertEquals(CART_ID, ((CheckedOut) checkout.event()).cartId());

    final EventSourcedBehaviorTestKit.CommandResultWithReply<ShoppingCartCommand, ShoppingCartEvent, ShoppingCartState, StatusReply<Summary>> item2 =
        eventSourcedTestKit.runCommand(replyTo -> new AddItem("foo2", 42, replyTo));

    assertTrue(item2.reply().isError());
  }
}