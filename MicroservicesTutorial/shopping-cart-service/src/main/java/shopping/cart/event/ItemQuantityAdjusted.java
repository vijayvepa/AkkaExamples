package shopping.cart.event;

import common.CborSerializable;
import shopping.cart.ShoppingCartEvent;

public record ItemQuantityAdjusted(String cartId, String itemId, int updatedQuantity) implements ShoppingCartEvent, CborSerializable {
}
