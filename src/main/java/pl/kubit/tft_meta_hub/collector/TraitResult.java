package pl.kubit.tft_meta_hub.collector;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * An active trait on a board, e.g. "DA_18_Fae" with 4 units at level 2. A value object without its own identity
 * ({@code @Embeddable}), stored in the {@code participant_trait} table of its participant.
 */
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class TraitResult {

	private String name;

	private int numUnits;

	/** Active level of the trait (1 = first breakpoint). */
	private int tierCurrent;

	private int style;
}
