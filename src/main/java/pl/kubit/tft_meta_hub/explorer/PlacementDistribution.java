package pl.kubit.tft_meta_hub.explorer;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Number of boards per placement 1–8 ({@code counts.get(0)} is 1st place).
 * The summary of the explorer and its bar chart are computed from it.
 */
public record PlacementDistribution(List<Long> counts) {

	public static final int PLACEMENTS = 8;

	public PlacementDistribution {
		if (counts.size() != PLACEMENTS) {
			throw new IllegalArgumentException("Expected " + PLACEMENTS + " placements but got " + counts.size());
		}
		counts = List.copyOf(counts);
	}

	public long games() {
		return counts.stream().mapToLong(Long::longValue).sum();
	}

	public long gamesAt(int placement) {
		return counts.get(placement - 1);
	}

	public Stats toStats() {
		long games = games();
		if (games == 0) {
			return Stats.EMPTY;
		}
		long placementSum = IntStream.rangeClosed(1, PLACEMENTS)
				.mapToLong(placement -> placement * gamesAt(placement))
				.sum();
		long top4 = IntStream.rangeClosed(1, 4).mapToLong(this::gamesAt).sum();
		return new Stats(games, (double) placementSum / games, (double) top4 / games, (double) gamesAt(1) / games);
	}
}
