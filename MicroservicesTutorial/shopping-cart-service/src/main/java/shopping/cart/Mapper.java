package shopping.cart;

import shopping.cart.model.ItemPopularity;
import shopping.cart.model.Summary;
import shopping.cart.proto.Cart;
import shopping.cart.proto.GetItemPopularityResponse;
import shopping.cart.proto.Item;

import java.util.List;
import java.util.Optional;

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

  public static GetItemPopularityResponse toProtoItemPopularity(Optional<ItemPopularity> itemPopularity) {
    long count = itemPopularity.map(ItemPopularity::count).orElse(0L);
    String id = itemPopularity.map(ItemPopularity::itemId).orElse("");
    return GetItemPopularityResponse.newBuilder()
        .setItemId(id).setPopularityCount(count).build();
  }
}
