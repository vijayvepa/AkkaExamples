package shopping.cart.event;

import shopping.cart.CborSerializable;

public record ItemRemoved(String cartId, String itemId) implements ShoppingCartEvent, CborSerializable {
}
