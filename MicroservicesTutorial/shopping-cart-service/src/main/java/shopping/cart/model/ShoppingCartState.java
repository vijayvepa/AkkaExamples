package shopping.cart.model;

import shopping.cart.CborSerializable;

import java.util.HashMap;
import java.util.Map;

public record ShoppingCartState(Map<String, Integer> items)  implements CborSerializable {
  public Summary toSummary(){
    return new Summary(new HashMap<>(items));
  }

  public int itemCount(String itemId){
    return items.get(itemId);
  }

  public boolean isEmpty(){
    return items.isEmpty();
  }

  public boolean hasItem(String itemId){
    return items.containsKey(itemId);
  }

  public ShoppingCartState updateItem(String itemId, int quantity){
    if(quantity == 0){
      items.remove(itemId);
      return this;
    }

    items.put(itemId, quantity);
    return this;
  }
}
