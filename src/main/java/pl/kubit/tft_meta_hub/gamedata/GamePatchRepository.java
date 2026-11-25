package pl.kubit.tft_meta_hub.gamedata;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface GamePatchRepository extends JpaRepository<GamePatch, Long> {

	List<GamePatch> findAllByOrderByStartsAtDesc();

	boolean existsByPatch(String patch);

	boolean existsByStartsAt(Instant startsAt);
}
