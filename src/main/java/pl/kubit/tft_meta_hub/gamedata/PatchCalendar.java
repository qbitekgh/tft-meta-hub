package pl.kubit.tft_meta_hub.gamedata;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * All known patches, newest first. Loaded once per collection run, so assigning a patch to hundreds of matches
 * needs no extra database queries.
 */
public record PatchCalendar(List<GamePatchView> patches) {

	public PatchCalendar {
		patches = patches.stream()
				.sorted(Comparator.comparing(GamePatchView::startsAt).reversed())
				.toList();
	}

	/** The patch that was live at {@code moment}: the one with the latest start that is not after it. */
	public Optional<GamePatchView> patchAt(Instant moment) {
		return patches.stream()
				.filter(patch -> !patch.startsAt().isAfter(moment))
				.findFirst();
	}

	/** The newest patch; its set is the set the collector accepts matches from. */
	public Optional<GamePatchView> latest() {
		return patches.stream().findFirst();
	}
}
