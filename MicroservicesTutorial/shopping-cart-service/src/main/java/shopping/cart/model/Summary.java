package shopping.cart.model;

import shopping.cart.CborSerializable;

import java.util.Map;

public record Summary(Map<String, Integer> items, boolean checkedOut) implements CborSerializable {
}
