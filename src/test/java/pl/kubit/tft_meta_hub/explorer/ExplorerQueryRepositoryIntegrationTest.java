package pl.kubit.tft_meta_hub.explorer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import pl.kubit.tft_meta_hub.TestcontainersConfiguration;
import pl.kubit.tft_meta_hub.explorer.ExplorerFilter.ItemFilter;
import pl.kubit.tft_meta_hub.explorer.ExplorerFilter.TraitFilter;
import pl.kubit.tft_meta_hub.explorer.ExplorerFilter.UnitFilter;
import pl.kubit.tft_meta_hub.explorer.ExplorerFilter.UnitItemFilter;
import pl.kubit.tft_meta_hub.riot.Platform;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;
import static pl.kubit.tft_meta_hub.explorer.ExplorerTestData.AHRI;
import static pl.kubit.tft_meta_hub.explorer.ExplorerTestData.BRAWLER;
import static pl.kubit.tft_meta_hub.explorer.ExplorerTestData.DEATHBLADE;
import static pl.kubit.tft_meta_hub.explorer.ExplorerTestData.GUINSOO;
import static pl.kubit.tft_meta_hub.explorer.ExplorerTestData.INFINITY_EDGE;
import static pl.kubit.tft_meta_hub.explorer.ExplorerTestData.KAYLE;
import static pl.kubit.tft_meta_hub.explorer.ExplorerTestData.ORNN;
import static pl.kubit.tft_meta_hub.explorer.ExplorerTestData.WARMOG;

/**
 * The explorer's SQL against a real PostgreSQL on a small, hand-made set of boards.
 * <pre>
 * match 1, EUNE, 20 Nov, patch 18.7
 *   board A, 1st: Ahri 2★ [Guinsoo, IE, Deathblade], Ornn 1★ []      Brawler 4 (level 2)
 *   board B, 4th: Ahri 1★ [Guinsoo]                                   Brawler 2 (level 1)
 *   board C, 8th: Ornn 2★ [Warmog], Kayle 4★ []
 * match 2, EUW, 21 Nov, patch 18.7
 *   board D, 2nd: Ahri 3★ [Guinsoo, IE, Deathblade], Ahri 1★ []       Brawler 4 (level 2)
 *   board E, 6th: Ornn 1★ [Guinsoo]
 * match 3, EUNE, 15 Nov, patch 18.6 (never selected: other patch)
 *   board F, 3rd: Ahri 1★ []
 * </pre>
 * Patch 18.7 selects boards A–E with placements 1, 4, 8, 2, 6.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class ExplorerQueryRepositoryIntegrationTest {

	private static final Instant SINCE = Instant.parse("2026-11-01T00:00:00Z");

	@Autowired
	private ExplorerQueryRepository repository;

	@Autowired
	private JdbcTemplate jdbc;

	@BeforeEach
	void insertBoards() {
		match("EUN1_1", Platform.EUNE, "2026-11-20T12:00:00Z", "18.7");
		long a = board("EUN1_1", 1);
		unit(a, 0, AHRI, 2, GUINSOO, INFINITY_EDGE, DEATHBLADE);
		unit(a, 1, ORNN, 1);
		trait(a, BRAWLER, 4, 2);
		long b = board("EUN1_1", 4);
		unit(b, 0, AHRI, 1, GUINSOO);
		trait(b, BRAWLER, 2, 1);
		long c = board("EUN1_1", 8);
		unit(c, 0, ORNN, 2, WARMOG);
		unit(c, 1, KAYLE, 4);

		match("EUW1_2", Platform.EUW, "2026-11-21T12:00:00Z", "18.7");
		long d = board("EUW1_2", 2);
		// items are listed in another order than on board A: the build must still be the same
		unit(d, 0, AHRI, 3, DEATHBLADE, GUINSOO, INFINITY_EDGE);
		unit(d, 1, AHRI, 1);
		trait(d, BRAWLER, 4, 2);
		long e = board("EUW1_2", 6);
		unit(e, 0, ORNN, 1, GUINSOO);

		match("EUN1_3", Platform.EUNE, "2026-11-15T12:00:00Z", "18.6");
		long f = board("EUN1_3", 3);
		unit(f, 0, AHRI, 1);
	}

	@Test
	void shouldCountPlacementsOfSelectedPatch_whenNoFilterIsSet() {
		PlacementDistribution placements = repository.placements(selection());

		assertThat(placements.counts()).containsExactly(1L, 1L, 0L, 1L, 0L, 1L, 0L, 1L);
		Stats stats = placements.toStats();
		assertThat(stats.games()).isEqualTo(5);
		assertThat(stats.avgPlacement()).isCloseTo(21.0 / 5, within(1e-9));
	}

	@Test
	void shouldSelectBoardsOfServerAndTimeRange_whenGeneralParametersAreSet() {
		assertThat(placementsOf(new BoardSelection("18.7", Platform.EUW, SINCE, List.of()))).containsExactly(2, 6);
		assertThat(placementsOf(new BoardSelection("18.7", null, Instant.parse("2026-11-21T00:00:00Z"), List.of())))
				.containsExactly(2, 6);
		assertThat(placementsOf(new BoardSelection("18.6", null, SINCE, List.of()))).containsExactly(3);
	}

	@Test
	void shouldSelectBoardsWithUnitInStarAndItemRange_whenUnitFilterIsSet() {
		assertThat(placementsOf(selection(new UnitFilter(AHRI, 2, 3, 0, 3)))).containsExactly(1, 2);
		// board B has a 1-star Ahri with one item; board D has a 1-star Ahri without items
		assertThat(placementsOf(selection(new UnitFilter(AHRI, 1, 1, 0, 0)))).containsExactly(2);
		assertThat(placementsOf(selection(new UnitFilter(AHRI, 1, 3, 3, 3)))).containsExactly(1, 2);
		// "3 stars" also finds the 4-star Kayle
		assertThat(placementsOf(selection(new UnitFilter(KAYLE, 3, 3, 0, 3)))).containsExactly(8);
	}

	@Test
	void shouldSelectBoardsWithItem_whenItemFiltersAreSet() {
		assertThat(placementsOf(selection(new UnitItemFilter(ORNN, GUINSOO)))).containsExactly(6);
		assertThat(placementsOf(selection(new ItemFilter(GUINSOO)))).containsExactly(1, 2, 4, 6);
		assertThat(placementsOf(selection(new ItemFilter(WARMOG)))).containsExactly(8);
	}

	@Test
	void shouldSelectBoardsWithEnoughTraitUnits_whenTraitFilterIsSet() {
		assertThat(placementsOf(selection(new TraitFilter(BRAWLER, 3)))).containsExactly(1, 2);
		assertThat(placementsOf(selection(new TraitFilter(BRAWLER, 2)))).containsExactly(1, 2, 4);
	}

	@Test
	void shouldCombineFiltersWithAnd_whenSeveralFiltersAreSet() {
		assertThat(placementsOf(selection(UnitFilter.of(AHRI), UnitFilter.of(ORNN)))).containsExactly(1);
		assertThat(placementsOf(selection(UnitFilter.of(KAYLE), new TraitFilter(BRAWLER, 2)))).isEmpty();
	}

	@Test
	void shouldCountEveryBoardOncePerUnit_whenUnitStatsAreQueried() {
		Map<String, Stats> units = byKey(repository.unitStats(selection(), false));

		// Ahri: boards A, B, D (1st, 4th, 2nd); board D has two Ahris but counts once
		assertStats(units.get(AHRI), 3, 7.0 / 3, 1.0, 1.0 / 3);
		// Ornn: boards A, C, E (1st, 8th, 6th)
		assertStats(units.get(ORNN), 3, 5.0, 1.0 / 3, 1.0 / 3);
		assertStats(units.get(KAYLE), 1, 8.0, 0.0, 0.0);
	}

	@Test
	void shouldReturnRowPerStarLevel_whenUnitStatsAreSplitByStars() {
		List<StatsRow> rows = repository.unitStats(selection(), true);

		assertThat(rows).filteredOn(row -> row.key().equals(AHRI))
				.extracting(StatsRow::level, row -> row.stats().games())
				.containsExactlyInAnyOrder(
						tuple(1, 2L),   // boards B and D
						tuple(2, 1L),   // board A
						tuple(3, 1L));  // board D
	}

	@Test
	void shouldOnlyLookAtSelectedBoards_whenStatsAreQueriedWithFilter() {
		Map<String, Stats> units = byKey(repository.unitStats(selection(new TraitFilter(BRAWLER, 4)), false));

		assertThat(units).containsOnlyKeys(AHRI, ORNN);
		assertStats(units.get(AHRI), 2, 1.5, 1.0, 0.5);
		assertStats(units.get(ORNN), 1, 1.0, 1.0, 1.0);
	}

	@Test
	void shouldCountItemsOnAnyUnitOrOnHolder_whenItemStatsAreQueried() {
		Map<String, Stats> anyUnit = byKey(repository.itemStats(selection(), null));
		Map<String, Stats> onAhri = byKey(repository.itemStats(selection(), AHRI));

		assertStats(anyUnit.get(GUINSOO), 4, (1 + 4 + 2 + 6) / 4.0, 0.75, 0.25);
		assertStats(anyUnit.get(WARMOG), 1, 8.0, 0.0, 0.0);
		assertStats(onAhri.get(GUINSOO), 3, 7.0 / 3, 1.0, 1.0 / 3);
		assertThat(onAhri).containsOnlyKeys(GUINSOO, INFINITY_EDGE, DEATHBLADE);
	}

	@Test
	void shouldGroupCompleteBuildsRegardlessOfItemOrder_whenBuildStatsAreQueried() {
		List<StatsRow> builds = repository.buildStats(selection(), AHRI);

		// board B's Ahri has only one item, so it is not a complete build
		assertThat(builds).hasSize(1);
		assertThat(builds.getFirst().key()).isEqualTo(String.join(",", DEATHBLADE, GUINSOO, INFINITY_EDGE));
		assertStats(builds.getFirst().stats(), 2, 1.5, 1.0, 0.5);
	}

	@Test
	void shouldReturnRowPerTraitLevel_whenTraitStatsAreQueried() {
		List<StatsRow> traits = repository.traitStats(selection());

		assertThat(traits).extracting(StatsRow::key).containsOnly(BRAWLER);
		Map<Integer, Stats> byLevel = traits.stream().collect(Collectors.toMap(StatsRow::level, StatsRow::stats));
		assertStats(byLevel.get(2), 2, 1.5, 1.0, 0.5);
		assertStats(byLevel.get(1), 1, 4.0, 1.0, 0.0);
	}

	private static BoardSelection selection(ExplorerFilter... filters) {
		return new BoardSelection("18.7", null, SINCE, List.of(filters));
	}

	/** Placements of the selected boards in ascending order, e.g. [1, 2]. */
	private List<Integer> placementsOf(BoardSelection selection) {
		PlacementDistribution placements = repository.placements(selection);
		return IntStream.rangeClosed(1, PlacementDistribution.PLACEMENTS)
				.boxed()
				.flatMap(placement -> Stream.generate(() -> placement)
						.limit(placements.gamesAt(placement)))
				.toList();
	}

	private static Map<String, Stats> byKey(List<StatsRow> rows) {
		return rows.stream().collect(Collectors.toMap(StatsRow::key, StatsRow::stats));
	}

	private static void assertStats(Stats stats, long games, double avgPlacement, double top4Rate, double winRate) {
		assertThat(stats).isNotNull();
		assertThat(stats.games()).isEqualTo(games);
		assertThat(stats.avgPlacement()).isCloseTo(avgPlacement, within(1e-9));
		assertThat(stats.top4Rate()).isCloseTo(top4Rate, within(1e-9));
		assertThat(stats.winRate()).isCloseTo(winRate, within(1e-9));
	}

	private void match(String id, Platform platform, String playedAt, String patch) {
		Timestamp time = Timestamp.from(Instant.parse(playedAt));
		jdbc.update("""
				INSERT INTO tft_match (id, platform, game_datetime, game_version, patch, set_number, collected_at)
				VALUES (?, ?, ?, 'test', ?, 18, ?)""", id, platform.name(), time, patch, time);
	}

	private long board(String matchId, int placement) {
		return jdbc.queryForObject("INSERT INTO participant (match_id, placement, level) VALUES (?, ?, 8) RETURNING id",
				Long.class, matchId, placement);
	}

	private void unit(long boardId, int slot, String characterId, int stars, String... items) {
		long unitId = jdbc.queryForObject("""
				INSERT INTO participant_unit (participant_id, slot, character_id, star_level)
				VALUES (?, ?, ?, ?) RETURNING id""", Long.class, boardId, slot, characterId, stars);
		for (int i = 0; i < items.length; i++) {
			jdbc.update("INSERT INTO participant_unit_item (unit_id, slot, item_name) VALUES (?, ?, ?)",
					unitId, i, items[i]);
		}
	}

	private void trait(long boardId, String name, int units, int level) {
		jdbc.update("""
				INSERT INTO participant_trait (participant_id, name, num_units, tier_current, style)
				VALUES (?, ?, ?, ?, 1)""", boardId, name, units, level);
	}
}
