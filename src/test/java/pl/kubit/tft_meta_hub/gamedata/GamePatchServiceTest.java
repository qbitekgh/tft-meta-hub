package pl.kubit.tft_meta_hub.gamedata;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class GamePatchServiceTest {

	@Mock
	private GamePatchRepository gamePatchRepository;

	@InjectMocks
	private GamePatchService service;

	@Test
	void shouldSaveStartTimeAsUtc_whenPatchIsAdded() {
		service.add(new GamePatchForm(" 18.2 ", 18, LocalDateTime.parse("2026-11-19T10:00")));

		then(gamePatchRepository).should().save(argThat(patch ->
				patch.getPatch().equals("18.2")
						&& patch.getSetNumber() == 18
						&& patch.getStartsAt().equals(Instant.parse("2026-11-19T10:00:00Z"))));
	}

	@Test
	void shouldRejectPatch_whenSameNumberExists() {
		given(gamePatchRepository.existsByPatch("18.2")).willReturn(true);

		assertThatThrownBy(() -> service.add(new GamePatchForm("18.2", 18, LocalDateTime.parse("2026-11-19T10:00"))))
				.isInstanceOf(GamePatchException.class)
				.hasMessage("Patch 18.2 already exists.");
		then(gamePatchRepository).should(never()).save(any());
	}

	@Test
	void shouldRejectPatch_whenAnotherPatchStartsAtSameTime() {
		given(gamePatchRepository.existsByStartsAt(Instant.parse("2026-11-19T10:00:00Z"))).willReturn(true);

		assertThatThrownBy(() -> service.add(new GamePatchForm("18.3", 18, LocalDateTime.parse("2026-11-19T10:00"))))
				.isInstanceOf(GamePatchException.class)
				.hasMessage("Another patch already starts at that time.");
	}
}
