package pl.kubit.tft_meta_hub.collector;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import pl.kubit.tft_meta_hub.TestcontainersConfiguration;
import pl.kubit.tft_meta_hub.riot.dto.MatchDto;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/** Saving and deleting matches against a real PostgreSQL: mappings, cascades and the database-side retention cascade. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class MatchPersistenceIntegrationTest {

	private static final Instant COLLECTED_AT = Instant.parse("2026-11-25T12:00:00Z");

	@Autowired
	private TftMatchRepository matchRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void shouldStoreWholeMatchWithoutPlayerIdentity_whenRealMatchIsSaved() {
		MatchDto dto = RiotFixtures.realMatch();
		matchRepository.save(MatchMapper.toEntity(dto, RiotFixtures.PATCHES, COLLECTED_AT));
		flushAndClear();

		TftMatch stored = matchRepository.findById("EUN1_4027724406").orElseThrow();
		assertThat(stored.getPatch()).isEqualTo("18.1");
		assertThat(stored.getParticipants()).extracting(Participant::getPlacement).containsExactly(1, 8);
		Participant winner = stored.getParticipants().getFirst();
		assertThat(winner.getUnits()).hasSize(9);
		assertThat(winner.getTraits()).hasSize(10);

		long itemsInDto = dto.info().participants().stream()
				.flatMap(participant -> participant.units().stream())
				.mapToLong(unit -> unit.itemNames().size())
				.sum();
		assertThat(countRows("participant_unit_item")).isEqualTo(itemsInDto);
		assertThat(countRows("participant")).isEqualTo(2);
	}

	@Test
	void shouldDeleteOldMatchWithAllChildRows_whenRetentionRuns() {
		matchRepository.save(MatchMapper.toEntity(RiotFixtures.realMatch(), RiotFixtures.PATCHES, COLLECTED_AT));
		flushAndClear();

		int deleted = matchRepository.deleteAllPlayedBefore(Instant.parse("2026-12-31T00:00:00Z"));

		assertThat(deleted).isEqualTo(1);
		assertThat(countRows("tft_match")).isZero();
		assertThat(countRows("participant")).isZero();
		assertThat(countRows("participant_unit")).isZero();
		assertThat(countRows("participant_unit_item")).isZero();
		assertThat(countRows("participant_trait")).isZero();
	}

	@Test
	void shouldKeepRecentMatch_whenRetentionCutoffIsOlder() {
		matchRepository.save(MatchMapper.toEntity(RiotFixtures.realMatch(), RiotFixtures.PATCHES, COLLECTED_AT));
		flushAndClear();

		assertThat(matchRepository.deleteAllPlayedBefore(Instant.parse("2026-11-01T00:00:00Z"))).isZero();
		assertThat(matchRepository.findLatestGameDatetime()).isNotNull();
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}

	private long countRows(String table) {
		return ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM " + table).getSingleResult()).longValue();
	}
}
