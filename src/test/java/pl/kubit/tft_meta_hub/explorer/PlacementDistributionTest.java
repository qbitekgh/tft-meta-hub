package pl.kubit.tft_meta_hub.explorer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class PlacementDistributionTest {

	@Test
	void shouldComputeStats_whenBoardsHaveDifferentPlacements() {
		// 1st twice, 4th once, 8th once: (1 + 1 + 4 + 8) / 4 = 3.5; top 4 = 3/4; win = 2/4
		PlacementDistribution distribution = new PlacementDistribution(List.of(2L, 0L, 0L, 1L, 0L, 0L, 0L, 1L));

		Stats stats = distribution.toStats();

		assertThat(stats.games()).isEqualTo(4);
		assertThat(stats.avgPlacement()).isCloseTo(3.5, within(1e-9));
		assertThat(stats.top4Rate()).isCloseTo(0.75, within(1e-9));
		assertThat(stats.winRate()).isCloseTo(0.5, within(1e-9));
	}

	@Test
	void shouldReturnEmptyStats_whenThereAreNoBoards() {
		PlacementDistribution distribution = new PlacementDistribution(List.of(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L));

		assertThat(distribution.games()).isZero();
		assertThat(distribution.toStats()).isEqualTo(Stats.EMPTY);
	}

	@Test
	void shouldThrow_whenNotEightPlacementsAreGiven() {
		assertThatThrownBy(() -> new PlacementDistribution(List.of(1L, 2L)))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldScaleBarsToMostCommonPlacement_whenSummaryIsBuilt() {
		PlacementDistribution distribution = new PlacementDistribution(List.of(4L, 2L, 0L, 0L, 0L, 0L, 0L, 2L));

		ExplorerPage.Summary summary = ExplorerService.summary(distribution, 16);

		assertThat(summary.share()).isCloseTo(0.5, within(1e-9));
		assertThat(summary.placements()).extracting(ExplorerPage.PlacementBar::barHeight)
				.containsExactly(100.0, 50.0, 0.0, 0.0, 0.0, 0.0, 0.0, 50.0);
		assertThat(summary.placements().getFirst().share()).isCloseTo(0.5, within(1e-9));
	}
}
