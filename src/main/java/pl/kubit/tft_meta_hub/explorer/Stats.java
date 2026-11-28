package pl.kubit.tft_meta_hub.explorer;

/**
 * How a group of boards placed.
 *
 * @param games        number of boards
 * @param avgPlacement average placement, 1.0–8.0 (lower is better)
 * @param top4Rate     share of boards placed 1–4, 0.0–1.0
 * @param winRate      share of boards placed 1st, 0.0–1.0
 */
public record Stats(long games, double avgPlacement, double top4Rate, double winRate) {

	public static final Stats EMPTY = new Stats(0, 0, 0, 0);
}
