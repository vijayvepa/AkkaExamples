package shopping.cart;

import shopping.cart.CborSerializable;

public interface ShoppingCartEvent extends CborSerializable {
  String cartId();
}
