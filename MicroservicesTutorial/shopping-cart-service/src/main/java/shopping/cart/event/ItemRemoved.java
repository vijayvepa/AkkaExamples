package shopping.cart.event;

import shopping.cart.CborSerializable;
import shopping.cart.ShoppingCartEvent;

public record ItemRemoved(String cartId, String itemId) implements ShoppingCartEvent, CborSerializable {
}
