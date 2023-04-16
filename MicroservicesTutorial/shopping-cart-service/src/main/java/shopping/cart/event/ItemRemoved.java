package shopping.cart.event;

import common.CborSerializable;
import shopping.cart.ShoppingCartEvent;

public record ItemRemoved(String cartId, String itemId) implements ShoppingCartEvent, CborSerializable {
}
