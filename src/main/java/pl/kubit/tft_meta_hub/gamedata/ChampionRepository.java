package pl.kubit.tft_meta_hub.gamedata;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChampionRepository extends JpaRepository<Champion, Long> {

	List<Champion> findAllBySetNumber(int setNumber);
}
