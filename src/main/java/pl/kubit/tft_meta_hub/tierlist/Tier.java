package pl.kubit.tft_meta_hub.tierlist;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Declaration order is the display order (S first, Situational last). */
@Getter
@RequiredArgsConstructor
public enum Tier {
	S("S"),
	A("A"),
	B("B"),
	C("C"),
	SITUATIONAL("Situational");

	private final String label;
}
