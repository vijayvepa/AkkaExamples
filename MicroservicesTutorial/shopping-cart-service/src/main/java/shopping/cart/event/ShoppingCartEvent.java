package shopping.cart.event;

import shopping.cart.CborSerializable;

public interface ShoppingCartEvent extends CborSerializable {
  String cartId();
}
