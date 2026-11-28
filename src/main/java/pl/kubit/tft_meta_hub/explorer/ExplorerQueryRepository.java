package pl.kubit.tft_meta_hub.explorer;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import pl.kubit.tft_meta_hub.explorer.ExplorerFilter.ItemFilter;
import pl.kubit.tft_meta_hub.explorer.ExplorerFilter.TraitFilter;
import pl.kubit.tft_meta_hub.explorer.ExplorerFilter.UnitFilter;
import pl.kubit.tft_meta_hub.explorer.ExplorerFilter.UnitItemFilter;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Statistics queries of the explorer, aggregated in SQL.
 * <p>
 * Every query starts with the same common table expression {@code boards}: boards (participants) of the selected
 * patch, server and time range that pass every filter. Each filter adds one {@code EXISTS} condition. The SQL text is
 * built only from fixed fragments; every value is a named parameter, never concatenated into the SQL.
 * <p>
 * A board can hold the same unit (or item) twice, so the queries first take distinct (board, key) pairs:
 * every board counts once per row.
 */
@Repository
@RequiredArgsConstructor
class ExplorerQueryRepository {

	/** The same statistics columns for every table of the explorer. */
	private static final String STATS_COLUMNS = """
			COUNT(*) AS games,
			AVG(placement) AS avg_placement,
			AVG(CASE WHEN placement <= 4 THEN 1.0 ELSE 0.0 END) AS top4_rate,
			AVG(CASE WHEN placement = 1 THEN 1.0 ELSE 0.0 END) AS win_rate""";

	private static final RowMapper<StatsRow> STATS_ROW = (rs, rowNum) -> new StatsRow(
			rs.getString("row_key"),
			rs.getInt("row_level"),
			new Stats(rs.getLong("games"), rs.getDouble("avg_placement"), rs.getDouble("top4_rate"),
					rs.getDouble("win_rate")));

	private final NamedParameterJdbcTemplate jdbc;

	/** Number of selected boards for each placement 1–8. */
	PlacementDistribution placements(BoardSelection selection) {
		BoardsQuery boards = BoardsQuery.of(selection);
		String sql = boards.sql() + """
				SELECT placement, COUNT(*) AS games
				FROM boards
				GROUP BY placement
				""";
		List<Long> counts = new ArrayList<>(Collections.nCopies(PlacementDistribution.PLACEMENTS, 0L));
		jdbc.query(sql, boards.params(), rs -> {
			counts.set(rs.getInt("placement") - 1, rs.getLong("games"));
		});
		return new PlacementDistribution(counts);
	}

	/** One row per champion, or per champion and star level when {@code splitByStars}. */
	List<StatsRow> unitStats(BoardSelection selection, boolean splitByStars) {
		BoardsQuery boards = BoardsQuery.of(selection);
		String level = splitByStars ? "u.star_level" : "0";
		String sql = boards.sql() + """
				SELECT row_key, row_level, %s
				FROM (SELECT DISTINCT b.id, b.placement, u.character_id AS row_key, %s AS row_level
				      FROM boards b
				      JOIN participant_unit u ON u.participant_id = b.id) board_units
				GROUP BY row_key, row_level
				""".formatted(STATS_COLUMNS, level);
		return jdbc.query(sql, boards.params(), STATS_ROW);
	}

	/** One row per item on any unit, or only on {@code holder} when it is not null. */
	List<StatsRow> itemStats(BoardSelection selection, String holder) {
		BoardsQuery boards = BoardsQuery.of(selection);
		String holderCondition = holder == null ? "" : "WHERE u.character_id = :holder";
		String sql = boards.sql() + """
				SELECT row_key, 0 AS row_level, %s
				FROM (SELECT DISTINCT b.id, b.placement, ui.item_name AS row_key
				      FROM boards b
				      JOIN participant_unit u ON u.participant_id = b.id
				      JOIN participant_unit_item ui ON ui.unit_id = u.id
				      %s) board_items
				GROUP BY row_key
				""".formatted(STATS_COLUMNS, holderCondition);
		return jdbc.query(sql, boards.params().addValue("holder", holder), STATS_ROW);
	}

	/**
	 * One row per complete build (exactly three items) on {@code holder}. The build key lists the item api names
	 * in alphabetical order, so the order in which the items were built does not matter.
	 */
	List<StatsRow> buildStats(BoardSelection selection, String holder) {
		BoardsQuery boards = BoardsQuery.of(selection);
		String sql = boards.sql() + """
				, unit_builds AS (
				    SELECT u.participant_id, STRING_AGG(ui.item_name, ',' ORDER BY ui.item_name) AS build
				    FROM participant_unit u
				    JOIN participant_unit_item ui ON ui.unit_id = u.id
				    WHERE u.character_id = :holder
				    GROUP BY u.id, u.participant_id
				    HAVING COUNT(*) = 3
				)
				SELECT row_key, 0 AS row_level, %s
				FROM (SELECT DISTINCT b.id, b.placement, ub.build AS row_key
				      FROM boards b
				      JOIN unit_builds ub ON ub.participant_id = b.id) board_builds
				GROUP BY row_key
				""".formatted(STATS_COLUMNS);
		return jdbc.query(sql, boards.params().addValue("holder", holder), STATS_ROW);
	}

	/** One row per trait and trait level; only active traits are stored, so every row is an active trait. */
	List<StatsRow> traitStats(BoardSelection selection) {
		BoardsQuery boards = BoardsQuery.of(selection);
		String sql = boards.sql() + """
				SELECT t.name AS row_key, t.tier_current AS row_level, %s
				FROM boards b
				JOIN participant_trait t ON t.participant_id = b.id
				GROUP BY t.name, t.tier_current
				""".formatted(STATS_COLUMNS);
		return jdbc.query(sql, boards.params(), STATS_ROW);
	}

	/** The {@code WITH boards AS (...)} part of a query and its parameters. */
	private record BoardsQuery(String sql, MapSqlParameterSource params) {

		static BoardsQuery of(BoardSelection selection) {
			MapSqlParameterSource params = new MapSqlParameterSource()
					.addValue("patch", selection.patch())
					.addValue("since", Timestamp.from(selection.since()));
			StringBuilder sql = new StringBuilder("""
					WITH boards AS (
					    SELECT p.id, p.placement
					    FROM participant p
					    JOIN tft_match m ON m.id = p.match_id
					    WHERE m.patch = :patch
					      AND m.game_datetime >= :since
					""");
			if (selection.platform() != null) {
				sql.append("      AND m.platform = :platform\n");
				params.addValue("platform", selection.platform().name());
			}
			List<ExplorerFilter> filters = selection.filters();
			for (int i = 0; i < filters.size(); i++) {
				// every filter gets its own parameter names (f0_unit, f1_unit, ...), so two filters never clash
				String prefix = "f" + i + "_";
				sql.append("      AND ").append(condition(filters.get(i), prefix, params)).append('\n');
			}
			sql.append(")\n");
			return new BoardsQuery(sql.toString(), params);
		}

		private static String condition(ExplorerFilter filter, String p, MapSqlParameterSource params) {
			return switch (filter) {
				case UnitFilter unit -> {
					params.addValue(p + "unit", unit.unit())
							.addValue(p + "min_stars", unit.minStars())
							// 3 stars means "3 or more": a few units can reach 4 stars
							.addValue(p + "max_stars", unit.maxStars() == ExplorerFilter.MAX_STARS ? 4 : unit.maxStars())
							.addValue(p + "min_items", unit.minItems())
							.addValue(p + "max_items", unit.maxItems());
					yield """
							EXISTS (SELECT 1 FROM participant_unit u
							        WHERE u.participant_id = p.id
							          AND u.character_id = :%1$sunit
							          AND u.star_level BETWEEN :%1$smin_stars AND :%1$smax_stars
							          AND (SELECT COUNT(*) FROM participant_unit_item ui WHERE ui.unit_id = u.id)
							              BETWEEN :%1$smin_items AND :%1$smax_items)""".formatted(p);
				}
				case UnitItemFilter unitItem -> {
					params.addValue(p + "unit", unitItem.unit()).addValue(p + "item", unitItem.item());
					yield """
							EXISTS (SELECT 1 FROM participant_unit u
							        JOIN participant_unit_item ui ON ui.unit_id = u.id
							        WHERE u.participant_id = p.id
							          AND u.character_id = :%1$sunit
							          AND ui.item_name = :%1$sitem)""".formatted(p);
				}
				case ItemFilter item -> {
					params.addValue(p + "item", item.item());
					yield """
							EXISTS (SELECT 1 FROM participant_unit u
							        JOIN participant_unit_item ui ON ui.unit_id = u.id
							        WHERE u.participant_id = p.id
							          AND ui.item_name = :%1$sitem)""".formatted(p);
				}
				case TraitFilter trait -> {
					params.addValue(p + "trait", trait.trait()).addValue(p + "min_units", trait.minUnits());
					yield """
							EXISTS (SELECT 1 FROM participant_trait t
							        WHERE t.participant_id = p.id
							          AND t.name = :%1$strait
							          AND t.num_units >= :%1$smin_units)""".formatted(p);
				}
			};
		}
	}
}
