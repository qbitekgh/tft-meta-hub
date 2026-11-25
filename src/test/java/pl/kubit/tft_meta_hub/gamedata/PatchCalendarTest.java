package pl.kubit.tft_meta_hub.gamedata;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PatchCalendarTest {

	private static final GamePatchView PATCH_18_1 = new GamePatchView(1, "18.1", 18, Instant.parse("2026-11-05T10:00:00Z"));
	private static final GamePatchView PATCH_18_2 = new GamePatchView(2, "18.2", 18, Instant.parse("2026-11-19T10:00:00Z"));
	private static final GamePatchView PATCH_19_1 = new GamePatchView(3, "19.1", 19, Instant.parse("2026-12-03T10:00:00Z"));

	private final PatchCalendar calendar = new PatchCalendar(List.of(PATCH_18_2, PATCH_19_1, PATCH_18_1));

	@Test
	void shouldReturnPatchLiveAtThatMoment_whenGameIsBetweenTwoPatches() {
		assertThat(calendar.patchAt(Instant.parse("2026-11-20T18:00:00Z"))).contains(PATCH_18_2);
	}

	@Test
	void shouldReturnNewPatch_whenGameEndsExactlyAtItsStart() {
		assertThat(calendar.patchAt(Instant.parse("2026-11-19T10:00:00Z"))).contains(PATCH_18_2);
	}

	@Test
	void shouldReturnEmpty_whenGameIsOlderThanEveryPatch() {
		assertThat(calendar.patchAt(Instant.parse("2026-11-01T00:00:00Z"))).isEmpty();
	}

	@Test
	void shouldReturnNewestPatchAsLatest_whenPatchesAreGivenInAnyOrder() {
		assertThat(calendar.latest()).contains(PATCH_19_1);
		assertThat(new PatchCalendar(List.of()).latest()).isEmpty();
	}
}
