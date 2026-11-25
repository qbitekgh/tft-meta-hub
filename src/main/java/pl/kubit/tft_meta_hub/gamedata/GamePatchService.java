package pl.kubit.tft_meta_hub.gamedata;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GamePatchService {

	private final GamePatchRepository gamePatchRepository;

	@Transactional(readOnly = true)
	public List<GamePatchView> findAll() {
		return gamePatchRepository.findAllByOrderByStartsAtDesc().stream()
				.map(GamePatchService::toView)
				.toList();
	}

	@Transactional(readOnly = true)
	public PatchCalendar getCalendar() {
		return new PatchCalendar(findAll());
	}

	/** @throws GamePatchException when the patch or its start time is already taken */
	public void add(GamePatchForm form) {
		String patch = form.getPatch().strip();
		Instant startsAt = form.getStartsAt().toInstant(ZoneOffset.UTC);
		if (gamePatchRepository.existsByPatch(patch)) {
			throw new GamePatchException("Patch " + patch + " already exists.");
		}
		if (gamePatchRepository.existsByStartsAt(startsAt)) {
			throw new GamePatchException("Another patch already starts at that time.");
		}
		gamePatchRepository.save(GamePatch.create(patch, form.getSetNumber(), startsAt));
		log.info("Patch {} (set {}) added, live since {}", patch, form.getSetNumber(), startsAt);
	}

	public void delete(long id) {
		gamePatchRepository.deleteById(id);
	}

	private static GamePatchView toView(GamePatch patch) {
		return new GamePatchView(patch.getId(), patch.getPatch(), patch.getSetNumber(), patch.getStartsAt());
	}
}
