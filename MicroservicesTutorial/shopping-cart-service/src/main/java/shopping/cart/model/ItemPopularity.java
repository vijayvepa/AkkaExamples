package shopping.cart.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

@SuppressWarnings("com.haulmont.jpb.EntityIdMissingInspection")
@Entity
@Table(name = "item_popularity")
public record ItemPopularity(
    @Id
    String itemId,
    @Version
    Long version,
    long count
) {

  public ItemPopularity() {
    this("", null, 0);
  }

  public ItemPopularity changeCount(long delta) {
    return new ItemPopularity(itemId, version, count + delta);
  }
}
