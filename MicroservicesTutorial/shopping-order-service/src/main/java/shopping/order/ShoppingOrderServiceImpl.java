package shopping.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shopping.order.proto.Item;
import shopping.order.proto.OrderRequest;
import shopping.order.proto.OrderResponse;
import shopping.order.proto.ShoppingOrderService;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class ShoppingOrderServiceImpl implements ShoppingOrderService {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Override
  public CompletionStage<OrderResponse> order(OrderRequest in) {
    Item totalItem = in.getItemsList().stream().reduce(Item.newBuilder().build(), (sum, item) -> Item.newBuilder().setQuantity(sum.getQuantity() + item.getQuantity()).build());

    logger.info("Order {} items from cart {}", totalItem.getQuantity(), in.getCartId());
    final OrderResponse orderResponse = OrderResponse.newBuilder().setOk(true).build();

    return CompletableFuture.completedFuture(orderResponse);
  }
}
