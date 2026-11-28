package pl.kubit.tft_meta_hub.explorer;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * One filter of the explorer. A filter selects boards (one player's board in one match); filters are combined with AND.
 * <p>
 * Every filter is written to the URL as one {@code f} parameter, so a link with filters can be shared:
 * <ul>
 *     <li>{@code unit:TFT18_Ahri:2-3:1-3} – Ahri with 2–3 stars and 1–3 items,</li>
 *     <li>{@code unititem:TFT18_Ahri:DA_GuinsoosRageblade} – Guinsoo's Rageblade on Ahri,</li>
 *     <li>{@code item:DA_18_EmblemFae} – the item anywhere on the board,</li>
 *     <li>{@code trait:DA_18_Brawler:4} – at least 4 Brawlers.</li>
 * </ul>
 */
public sealed interface ExplorerFilter {

	/** Api names are made of letters, digits and underscores, so they never contain the ':' separator. */
	Pattern API_NAME = Pattern.compile("[A-Za-z0-9_]{1,100}");

	int MAX_STARS = 3;
	int MAX_ITEMS = 3;

	/** The value of the {@code f} URL parameter. */
	String toParam();

	/** @return the filter, or empty when the value is not a valid filter (e.g. an edited or outdated link) */
	static Optional<ExplorerFilter> parse(String param) {
		if (param == null) {
			return Optional.empty();
		}
		String[] parts = param.strip().split(":", -1);
		try {
			return Optional.of(switch (parts[0]) {
				case "unit" -> {
					requireParts(parts, 4);
					int[] stars = range(parts[2]);
					int[] items = range(parts[3]);
					yield new UnitFilter(parts[1], stars[0], stars[1], items[0], items[1]);
				}
				case "unititem" -> {
					requireParts(parts, 3);
					yield new UnitItemFilter(parts[1], parts[2]);
				}
				case "item" -> {
					requireParts(parts, 2);
					yield new ItemFilter(parts[1]);
				}
				case "trait" -> {
					requireParts(parts, 3);
					yield new TraitFilter(parts[1], Integer.parseInt(parts[2]));
				}
				default -> throw new IllegalArgumentException("Unknown filter type: " + parts[0]);
			});
		} catch (IllegalArgumentException e) {
			// NumberFormatException is an IllegalArgumentException too
			return Optional.empty();
		}
	}

	/**
	 * A champion on the board. {@code maxStars} 3 also matches the rare 4-star units, which count as 3+.
	 */
	record UnitFilter(String unit, int minStars, int maxStars, int minItems, int maxItems) implements ExplorerFilter {

		public UnitFilter {
			requireApiName(unit);
			requireRange(minStars, maxStars, 1, MAX_STARS, "stars");
			requireRange(minItems, maxItems, 0, MAX_ITEMS, "items");
		}

		/** Any star level and any number of items. */
		public static UnitFilter of(String unit) {
			return new UnitFilter(unit, 1, MAX_STARS, 0, MAX_ITEMS);
		}

		@Override
		public String toParam() {
			return "unit:" + unit + ":" + minStars + "-" + maxStars + ":" + minItems + "-" + maxItems;
		}
	}

	record UnitItemFilter(String unit, String item) implements ExplorerFilter {

		public UnitItemFilter {
			requireApiName(unit);
			requireApiName(item);
		}

		@Override
		public String toParam() {
			return "unititem:" + unit + ":" + item;
		}
	}

	record ItemFilter(String item) implements ExplorerFilter {

		public ItemFilter {
			requireApiName(item);
		}

		@Override
		public String toParam() {
			return "item:" + item;
		}
	}

	/** An active trait with at least {@code minUnits} units, e.g. Brawler 4+. */
	record TraitFilter(String trait, int minUnits) implements ExplorerFilter {

		public TraitFilter {
			requireApiName(trait);
			if (minUnits < 1 || minUnits > 20) {
				throw new IllegalArgumentException("Trait unit count out of range: " + minUnits);
			}
		}

		@Override
		public String toParam() {
			return "trait:" + trait + ":" + minUnits;
		}
	}

	private static void requireParts(String[] parts, int expected) {
		if (parts.length != expected) {
			throw new IllegalArgumentException("Expected " + expected + " parts but got " + parts.length);
		}
	}

	/** Parses "2-3" into {2, 3}. */
	private static int[] range(String value) {
		String[] bounds = value.split("-", -1);
		if (bounds.length != 2) {
			throw new IllegalArgumentException("Not a range: " + value);
		}
		return new int[]{Integer.parseInt(bounds[0]), Integer.parseInt(bounds[1])};
	}

	private static void requireApiName(String apiName) {
		if (apiName == null || !API_NAME.matcher(apiName).matches()) {
			throw new IllegalArgumentException("Invalid api name: " + apiName);
		}
	}

	private static void requireRange(int min, int max, int lowest, int highest, String what) {
		if (min < lowest || max > highest || min > max) {
			throw new IllegalArgumentException("Invalid " + what + " range: " + min + "-" + max);
		}
	}
}
