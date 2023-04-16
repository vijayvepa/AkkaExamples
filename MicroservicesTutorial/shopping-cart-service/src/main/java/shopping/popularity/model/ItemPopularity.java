package shopping.popularity.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.Objects;

@Entity
@Table(name = "item_popularity")
public final class ItemPopularity {
  @Id
  private final String itemId;
  @Version
  private final Long version;
  private final long count;

  public ItemPopularity(
      String itemId,
      Long version,
      long count
  ) {
    this.itemId = itemId;
    this.version = version;
    this.count = count;
  }

  public ItemPopularity() {
    this("", null, 0);
  }

  public ItemPopularity changeCount(long delta) {
    return new ItemPopularity(itemId, version, count + delta);
  }

  @Id
  public String itemId() {
    return itemId;
  }

  @Version
  public Long version() {
    return version;
  }

  public long count() {
    return count;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (ItemPopularity) obj;
    return Objects.equals(this.itemId, that.itemId) &&
        Objects.equals(this.version, that.version) &&
        this.count == that.count;
  }

  @Override
  public int hashCode() {
    return Objects.hash(itemId, version, count);
  }

  @Override
  public String toString() {
    return "ItemPopularity[" +
        "itemId=" + itemId + ", " +
        "version=" + version + ", " +
        "count=" + count + ']';
  }

}
