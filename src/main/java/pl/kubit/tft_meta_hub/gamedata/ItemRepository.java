package pl.kubit.tft_meta_hub.gamedata;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

	List<Item> findAllBySetNumber(int setNumber);
}
