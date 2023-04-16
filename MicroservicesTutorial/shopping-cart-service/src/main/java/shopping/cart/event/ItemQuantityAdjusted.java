package shopping.cart.event;

import shopping.cart.CborSerializable;
import shopping.cart.ShoppingCartEvent;

public record ItemQuantityAdjusted(String cartId, String itemId, int updatedQuantity) implements ShoppingCartEvent, CborSerializable {
}
