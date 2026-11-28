package pl.kubit.tft_meta_hub.explorer;

import org.junit.jupiter.api.Test;
import pl.kubit.tft_meta_hub.explorer.ExplorerFilter.ItemFilter;
import pl.kubit.tft_meta_hub.explorer.ExplorerFilter.TraitFilter;
import pl.kubit.tft_meta_hub.explorer.ExplorerFilter.UnitFilter;
import pl.kubit.tft_meta_hub.explorer.ExplorerFilter.UnitItemFilter;
import pl.kubit.tft_meta_hub.explorer.ExplorerPage.ExplorerRow;
import pl.kubit.tft_meta_hub.explorer.ExplorerPage.RowIcon;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static pl.kubit.tft_meta_hub.explorer.ExplorerTestData.AHRI;
import static pl.kubit.tft_meta_hub.explorer.ExplorerTestData.BRAWLER;
import static pl.kubit.tft_meta_hub.explorer.ExplorerTestData.DEATHBLADE;
import static pl.kubit.tft_meta_hub.explorer.ExplorerTestData.GUINSOO;
import static pl.kubit.tft_meta_hub.explorer.ExplorerTestData.INFINITY_EDGE;
import static pl.kubit.tft_meta_hub.explorer.ExplorerTestData.KAYLE;
import static pl.kubit.tft_meta_hub.explorer.ExplorerTestData.ORNN;
import static pl.kubit.tft_meta_hub.explorer.ExplorerTestData.POTION;
import static pl.kubit.tft_meta_hub.explorer.ExplorerTestData.WARMOG;
import static pl.kubit.tft_meta_hub.explorer.ExplorerTestData.catalog;
import static pl.kubit.tft_meta_hub.explorer.ExplorerTestData.row;
import static pl.kubit.tft_meta_hub.explorer.ExplorerTestData.state;

class ExplorerRowsTest {

	@Test
	void shouldSortByAveragePlacementAndUseGameData_whenUnitsAreShown() {
		List<StatsRow> stats = List.of(row(ORNN, 0, 40, 4.8), row(AHRI, 0, 20, 3.9));

		List<ExplorerRow> rows = ExplorerRows.build(state(ExplorerTab.UNITS), stats, catalog(), 80);

		assertThat(rows).extracting(ExplorerRow::name).containsExactly("Ahri", "Ornn");
		ExplorerRow ahri = rows.getFirst();
		assertThat(ahri.icons()).containsExactly(new RowIcon("Ahri", "https://cdn.test/ahri.png", 4, 0));
		assertThat(ahri.frequency()).isCloseTo(0.25, within(1e-9));
		assertThat(ahri.filtersToAdd()).containsExactly(UnitFilter.of(AHRI));
	}

	@Test
	void shouldLeaveOutRows_whenTheyHaveTooFewGames() {
		ExplorerState state = state(ExplorerTab.UNITS);
		List<StatsRow> stats = List.of(row(AHRI, 0, state.minGames() - 1, 1.0), row(ORNN, 0, state.minGames(), 5.0));

		assertThat(ExplorerRows.build(state, stats, catalog(), 100)).extracting(ExplorerRow::name).containsExactly("Ornn");
	}

	@Test
	void shouldShowOnlyChampionsOfThatCost_whenCostIsChosen() {
		ExplorerState state = ExplorerState.from(new ExplorerRequest(null, null, null, null, null, null, null, 5,
				null, null), List.of("18.7"));
		List<StatsRow> stats = List.of(row(AHRI, 0, 20, 4.0), row(KAYLE, 0, 20, 4.5));

		assertThat(ExplorerRows.build(state, stats, catalog(), 100)).extracting(ExplorerRow::name).containsExactly("Kayle");
	}

	@Test
	void shouldPutMostPlayedFirst_whenSortedByGames() {
		ExplorerState state = state(ExplorerTab.UNITS).withSort(SortColumn.GAMES);
		List<StatsRow> stats = List.of(row(AHRI, 0, 20, 3.0), row(ORNN, 0, 60, 5.0));

		assertThat(ExplorerRows.build(state, stats, catalog(), 100)).extracting(ExplorerRow::name).containsExactly("Ornn", "Ahri");
	}

	@Test
	void shouldAddStarLevelToNameAndFilter_whenSplitByStars() {
		List<StatsRow> stats = List.of(row(AHRI, 2, 20, 4.0), row(AHRI, 4, 10, 1.0));

		List<ExplorerRow> rows = ExplorerRows.build(state(ExplorerTab.UNITS), stats, catalog(), 100);

		assertThat(rows).extracting(ExplorerRow::name).containsExactly("Ahri 4★", "Ahri 2★");
		// a 4-star unit is found by the "3 stars" filter, which means 3 or more
		assertThat(rows.getFirst().filtersToAdd()).containsExactly(new UnitFilter(AHRI, 3, 3, 0, 3));
		assertThat(rows.get(1).filtersToAdd()).containsExactly(new UnitFilter(AHRI, 2, 2, 0, 3));
	}

	@Test
	void shouldLeaveOutItemsMissingFromGameData_whenItemsAreShown() {
		List<StatsRow> stats = List.of(row(GUINSOO, 0, 30, 4.2), row(POTION, 0, 30, 3.0));

		List<ExplorerRow> rows = ExplorerRows.build(state(ExplorerTab.ITEMS), stats, catalog(), 100);

		assertThat(rows).extracting(ExplorerRow::name).containsExactly("Guinsoo's Rageblade");
		assertThat(rows.getFirst().filtersToAdd()).containsExactly(new ItemFilter(GUINSOO));
	}

	@Test
	void shouldAddItemOnHolderFilter_whenItemHolderIsChosen() {
		ExplorerState state = ExplorerState.from(new ExplorerRequest(null, null, null, null, "ITEMS", AHRI, null, null,
				null, null), List.of("18.7"));

		List<ExplorerRow> rows = ExplorerRows.build(state, List.of(row(GUINSOO, 0, 30, 4.2)), catalog(), 100);

		assertThat(rows.getFirst().filtersToAdd()).containsExactly(new UnitItemFilter(AHRI, GUINSOO));
	}

	@Test
	void shouldShowThreeItemsAndAddThreeFilters_whenBuildIsShown() {
		ExplorerState state = ExplorerState.from(new ExplorerRequest(null, null, null, null, "BUILDS", AHRI, null, null,
				null, null), List.of("18.7"));
		String build = String.join(",", DEATHBLADE, GUINSOO, INFINITY_EDGE);
		String buildWithPotion = String.join(",", GUINSOO, POTION, WARMOG);

		List<ExplorerRow> rows = ExplorerRows.build(state,
				List.of(row(build, 0, 15, 3.1), row(buildWithPotion, 0, 15, 3.0)), catalog(), 100);

		assertThat(rows).hasSize(1);
		ExplorerRow row = rows.getFirst();
		assertThat(row.name()).isEqualTo("Deathblade, Guinsoo's Rageblade, Infinity Edge");
		assertThat(row.icons()).hasSize(3);
		assertThat(row.filtersToAdd()).containsExactly(new UnitItemFilter(AHRI, DEATHBLADE),
				new UnitItemFilter(AHRI, GUINSOO), new UnitItemFilter(AHRI, INFINITY_EDGE));
	}

	@Test
	void shouldNameTraitByUnitsOfItsLevel_whenTraitsAreShown() {
		List<StatsRow> stats = List.of(row(BRAWLER, 2, 30, 3.5), row(BRAWLER, 7, 10, 2.0));

		List<ExplorerRow> rows = ExplorerRows.build(state(ExplorerTab.TRAITS), stats, catalog(), 100);

		assertThat(rows).extracting(ExplorerRow::name).containsExactly("Brawler (level 7)", "4 Brawler");
		assertThat(rows.get(1).filtersToAdd()).containsExactly(new TraitFilter(BRAWLER, 4));
	}
}
