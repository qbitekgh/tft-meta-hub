package pl.kubit.tft_meta_hub.explorer;

import pl.kubit.tft_meta_hub.explorer.ExplorerFilter.ItemFilter;
import pl.kubit.tft_meta_hub.explorer.ExplorerFilter.TraitFilter;
import pl.kubit.tft_meta_hub.explorer.ExplorerFilter.UnitFilter;
import pl.kubit.tft_meta_hub.explorer.ExplorerFilter.UnitItemFilter;

import java.util.Optional;

/**
 * Fields of one "add filter" form. {@code type} says which form was sent: unit, unititem, item or trait.
 * Missing ranges mean "any" (1–3 stars, 0–3 items, 1+ units).
 */
public record NewFilterRequest(String type,
							   String unit,
							   Integer minStars,
							   Integer maxStars,
							   Integer minItems,
							   Integer maxItems,
							   String item,
							   String trait,
							   Integer minUnits) {

	/** @return the filter, or empty when the form was incomplete or its values are invalid */
	public Optional<ExplorerFilter> toFilter() {
		if (type == null) {
			return Optional.empty();
		}
		try {
			return Optional.of(switch (type) {
				case "unit" -> new UnitFilter(unit,
						orDefault(minStars, 1), orDefault(maxStars, ExplorerFilter.MAX_STARS),
						orDefault(minItems, 0), orDefault(maxItems, ExplorerFilter.MAX_ITEMS));
				case "unititem" -> new UnitItemFilter(unit, item);
				case "item" -> new ItemFilter(item);
				case "trait" -> new TraitFilter(trait, orDefault(minUnits, 1));
				default -> throw new IllegalArgumentException("Unknown filter type: " + type);
			});
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	private static int orDefault(Integer value, int defaultValue) {
		return value == null ? defaultValue : value;
	}
}
