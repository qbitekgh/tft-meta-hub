package pl.kubit.tft_meta_hub.explorer;

/**
 * One row of a statistics query, before game data turns it into names and icons.
 *
 * @param key   api name of the unit, item or trait; for builds the three item api names joined with ','
 * @param level star level (units split by star level) or trait level; 0 when the row has no level
 */
public record StatsRow(String key, int level, Stats stats) {
}
