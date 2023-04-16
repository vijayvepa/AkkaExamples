package shopping.cart;

import common.CborSerializable;

public interface ShoppingCartEvent extends CborSerializable {
  String cartId();
}
