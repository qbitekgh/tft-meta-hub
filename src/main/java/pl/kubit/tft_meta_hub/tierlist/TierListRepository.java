package pl.kubit.tft_meta_hub.tierlist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TierListRepository extends JpaRepository<TierList, Long> {

	Optional<TierList> findByStatus(TierListStatus status);

	List<TierList> findAllByOrderByUpdatedAtDesc();
}
