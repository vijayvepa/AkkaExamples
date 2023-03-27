package shopping.cart.event;

import shopping.cart.CborSerializable;

public record ItemAdded(String cartId, String itemId, int quantity) implements ShoppingCartEvent, CborSerializable {

}
