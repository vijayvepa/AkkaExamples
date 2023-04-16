package shopping.cart.event;

import common.CborSerializable;
import shopping.cart.ShoppingCartEvent;

public record ItemAdded(String cartId, String itemId, int quantity) implements ShoppingCartEvent, CborSerializable {

}
