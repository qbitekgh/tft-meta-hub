package pl.kubit.tft_meta_hub.collector;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackedPlayerRepository extends JpaRepository<TrackedPlayer, String> {
}
