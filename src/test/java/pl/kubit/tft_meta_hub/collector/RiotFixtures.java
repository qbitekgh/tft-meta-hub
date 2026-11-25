package pl.kubit.tft_meta_hub.collector;

import pl.kubit.tft_meta_hub.gamedata.GamePatchView;
import pl.kubit.tft_meta_hub.gamedata.PatchCalendar;
import pl.kubit.tft_meta_hub.riot.dto.MatchDto;
import pl.kubit.tft_meta_hub.riot.dto.MatchDto.InfoDto;
import pl.kubit.tft_meta_hub.riot.dto.MatchDto.MetadataDto;
import pl.kubit.tft_meta_hub.riot.dto.MatchDto.ParticipantDto;
import pl.kubit.tft_meta_hub.riot.dto.MatchDto.TraitDto;
import pl.kubit.tft_meta_hub.riot.dto.MatchDto.UnitDto;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;

/** Test data for the collector: the anonymised real match from {@code riot/match.json} and small hand-made matches. */
final class RiotFixtures {

	/** The real match ended 2026-11-12; patch 18.1 was live then. */
	static final PatchCalendar PATCHES = new PatchCalendar(List.of(
			new GamePatchView(1, "18.1", 18, Instant.parse("2026-11-05T10:00:00Z")),
			new GamePatchView(2, "18.2", 18, Instant.parse("2026-11-19T10:00:00Z"))));

	private RiotFixtures() {
	}

	static MatchDto realMatch() {
		try (InputStream input = RiotFixtures.class.getResourceAsStream("/riot/match.json")) {
			return JsonMapper.builder().build().readValue(input, MatchDto.class);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/** A small match with one board: queue and set are what the collector filters on. */
	static MatchDto match(String matchId, int queueId, int setNumber, Instant gameEnd) {
		ParticipantDto board = new ParticipantDto(1, 9,
				List.of(new UnitDto("DA_18_Xayah", 3, List.of("DA_GuinsoosRageblade"))),
				List.of(new TraitDto("DA_18_Fae", 2, 1, 1), new TraitDto("DA_18_Coven", 1, 0, 0)));
		return new MatchDto(new MetadataDto(matchId),
				new InfoDto(gameEnd.toEpochMilli(), "TFT Unreal Version ?.?.?.?", queueId, setNumber, List.of(board)));
	}
}
