package pl.kubit.tft_meta_hub.explorer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExplorerTab {
	UNITS("Units", "Unit"),
	ITEMS("Items", "Item"),
	BUILDS("Builds", "Build"),
	TRAITS("Traits", "Trait");

	private final String label;

	/** Header of the table's first column. */
	private final String columnLabel;
}
