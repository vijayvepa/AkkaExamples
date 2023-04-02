package shopping.cart;

import akka.projection.eventsourced.EventEnvelope;
import akka.projection.jdbc.javadsl.JdbcHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shopping.cart.event.ItemAdded;
import shopping.cart.event.ShoppingCartEvent;
import shopping.cart.model.ItemPopularity;
import shopping.cart.repository.HibernateJdbcSession;
import shopping.cart.repository.ItemPopularityRepository;
public class ItemPopularityProjectionHandler extends JdbcHandler<EventEnvelope<ShoppingCartEvent>, HibernateJdbcSession> {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final String tag;
  private final ItemPopularityRepository repository;

  public ItemPopularityProjectionHandler(
      String tag,
      ItemPopularityRepository repository) {
    this.tag = tag;
    this.repository = repository;
  }

  private ItemPopularity findOrNew(String itemId){
    return repository.findById(itemId).orElseGet(()-> new ItemPopularity(itemId, 0L, 0));
  }

  @Override
  public void process(
      HibernateJdbcSession session,
      EventEnvelope<ShoppingCartEvent> shoppingCartEventEventEnvelope) {

    final ShoppingCartEvent event = shoppingCartEventEventEnvelope.event();

    if(!(event instanceof final ItemAdded itemAdded)){
      return;
    }

    final String itemId = itemAdded.itemId();


    final ItemPopularity existingItemPopularity = findOrNew(itemId);
    final ItemPopularity updatedItemPopularity = existingItemPopularity.changeCount(itemAdded.quantity());

    repository.save(updatedItemPopularity);

    logger.info("ItemPopularityProjectionHandler({}) item popularity for '{}' : [{}]" , this.tag, itemId, updatedItemPopularity.count());

  }
}
