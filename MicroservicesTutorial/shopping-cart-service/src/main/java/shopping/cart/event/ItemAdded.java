package shopping.cart.event;

import shopping.cart.CborSerializable;
import shopping.cart.ShoppingCartEvent;

public record ItemAdded(String cartId, String itemId, int quantity) implements ShoppingCartEvent, CborSerializable {

}
