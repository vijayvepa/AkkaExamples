package shopping.popularity.repository;

import org.springframework.data.repository.Repository;
import shopping.popularity.model.ItemPopularity;

import java.util.Optional;

public interface ItemPopularityRepository extends Repository<ItemPopularity, String> {

  ItemPopularity save(ItemPopularity itemPopularity);
  Optional<ItemPopularity> findById(String id);

}
