package pl.kubit.tft_meta_hub.explorer;

import java.util.Comparator;

/** Columns a statistics table can be sorted by; each column sorts best values first. */
public enum SortColumn {
	AVG_PLACE(Comparator.comparingDouble(Stats::avgPlacement)),
	TOP4(Comparator.comparingDouble(Stats::top4Rate).reversed()),
	WIN(Comparator.comparingDouble(Stats::winRate).reversed()),
	GAMES(Comparator.comparingLong(Stats::games).reversed());

	private final Comparator<Stats> bestFirst;

	SortColumn(Comparator<Stats> bestFirst) {
		this.bestFirst = bestFirst;
	}

	/** Best first; ties go to the row with more games, which is the more reliable one. */
	Comparator<Stats> comparator() {
		return bestFirst.thenComparing(Comparator.comparingLong(Stats::games).reversed());
	}
}
