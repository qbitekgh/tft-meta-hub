package pl.kubit.tft_meta_hub.collector;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

public interface TftMatchRepository extends JpaRepository<TftMatch, String> {

	/**
	 * Bulk delete with one SQL statement. JPA does not cascade bulk deletes, so participants, units, items and traits
	 * are removed by the database itself ({@code ON DELETE CASCADE} in the migration).
	 */
	@Modifying
	@Transactional
	@Query("delete from TftMatch m where m.gameDatetime < :cutoff")
	int deleteAllPlayedBefore(@Param("cutoff") Instant cutoff);

	@Query("select max(m.gameDatetime) from TftMatch m")
	Instant findLatestGameDatetime();
}
