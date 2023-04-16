package shopping.cart.event;

import shopping.cart.ShoppingCartEvent;

import java.time.Instant;

public record CheckedOut(String cartId, Instant eventTime) implements ShoppingCartEvent {
}
