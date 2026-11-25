package pl.kubit.tft_meta_hub.tierlist;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PlayStyle {
	FAST_8("Fast 8"),
	REROLL_1_COST("1-cost reroll"),
	REROLL_2_COST("2-cost reroll"),
	REROLL_3_COST("3-cost reroll"),
	FLEX("Flex");

	private final String label;
}
