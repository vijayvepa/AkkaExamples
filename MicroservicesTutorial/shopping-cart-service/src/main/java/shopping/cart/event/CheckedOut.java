package shopping.cart.event;

import java.time.Instant;

public record CheckedOut(String cartId, Instant eventTime) implements ShoppingCartEvent {
}
