package shopping.cart;

import shopping.cart.model.Summary;
import shopping.cart.proto.Cart;
import shopping.cart.proto.Item;

import java.util.List;

public class Mapper {
  private Mapper(){}

  public static Cart toProtoSummary(Summary summary) {
    final List<Item> protoItems = summary.items().entrySet().stream()
        .map(entry -> Item.newBuilder()
            .setItemId(entry.getKey())
            .setQuantity(entry.getValue())
            .build()).toList();

    return Cart.newBuilder().addAllItems(protoItems).build();
  }
}
