package shopping.cart.event;

import shopping.cart.CborSerializable;

public record ItemQuantityAdjusted(String cartId, String itemId, int updatedQuantity) implements ShoppingCartEvent, CborSerializable {
}
