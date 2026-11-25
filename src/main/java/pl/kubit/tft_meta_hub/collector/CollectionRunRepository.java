package pl.kubit.tft_meta_hub.collector;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CollectionRunRepository extends JpaRepository<CollectionRun, Long> {

	Optional<CollectionRun> findFirstByOrderByStartedAtDesc();

	List<CollectionRun> findAllByStatus(RunStatus status);
}
