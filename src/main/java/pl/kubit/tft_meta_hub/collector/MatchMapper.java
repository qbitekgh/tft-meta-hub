package pl.kubit.tft_meta_hub.collector;

import pl.kubit.tft_meta_hub.gamedata.GamePatchView;
import pl.kubit.tft_meta_hub.gamedata.PatchCalendar;
import pl.kubit.tft_meta_hub.riot.Platform;
import pl.kubit.tft_meta_hub.riot.dto.MatchDto;
import pl.kubit.tft_meta_hub.riot.dto.MatchDto.InfoDto;
import pl.kubit.tft_meta_hub.riot.dto.MatchDto.ParticipantDto;
import pl.kubit.tft_meta_hub.riot.dto.MatchDto.TraitDto;
import pl.kubit.tft_meta_hub.riot.dto.MatchDto.UnitDto;

import java.time.Instant;
import java.util.List;

/** Turns Riot's match response into the entities we store. Pure logic, no Spring, no database. */
final class MatchMapper {

	static final int RANKED_QUEUE_ID = 1100;

	private MatchMapper() {
	}

	/** Only ranked games of the current set are stored (no Double Up, normal games or older sets). */
	static boolean isCollectable(MatchDto match, int currentSet) {
		return match.info().queueId() == RANKED_QUEUE_ID && match.info().tftSetNumber() == currentSet;
	}

	static TftMatch toEntity(MatchDto dto, PatchCalendar patches, Instant collectedAt) {
		String matchId = dto.metadata().matchId();
		InfoDto info = dto.info();
		Platform platform = Platform.fromMatchId(matchId)
				.orElseThrow(() -> new IllegalArgumentException("Unknown platform in match id " + matchId));
		Instant gameDatetime = Instant.ofEpochMilli(info.gameDatetime());
		String patch = patches.patchAt(gameDatetime).map(GamePatchView::patch).orElse(null);

		TftMatch match = TftMatch.create(matchId, platform, gameDatetime, info.gameVersion(), patch,
				info.tftSetNumber(), collectedAt);
		for (ParticipantDto participantDto : info.participants()) {
			Participant participant = match.addParticipant(participantDto.placement(), participantDto.level());
			for (UnitDto unit : nonNull(participantDto.units())) {
				participant.addUnit(unit.characterId(), unit.starLevel(), nonNull(unit.itemNames()));
			}
			nonNull(participantDto.traits()).stream()
					.filter(TraitDto::isActive)
					.map(trait -> new TraitResult(trait.name(), trait.numUnits(), trait.tierCurrent(), trait.style()))
					.forEach(participant::addTrait);
		}
		return match;
	}

	private static <T> List<T> nonNull(List<T> list) {
		return list == null ? List.of() : list;
	}
}
