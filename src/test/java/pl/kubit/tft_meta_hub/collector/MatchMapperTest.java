package pl.kubit.tft_meta_hub.collector;

import org.junit.jupiter.api.Test;
import pl.kubit.tft_meta_hub.riot.Platform;
import pl.kubit.tft_meta_hub.riot.dto.MatchDto;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchMapperTest {

	private static final Instant COLLECTED_AT = Instant.parse("2026-11-25T12:00:00Z");

	@Test
	void shouldMapBoardsOfRealMatch_whenMatchIsRanked() {
		MatchDto dto = RiotFixtures.realMatch();

		TftMatch match = MatchMapper.toEntity(dto, RiotFixtures.PATCHES, COLLECTED_AT);

		assertThat(match.getId()).isEqualTo("EUN1_4027724406");
		assertThat(match.getPlatform()).isEqualTo(Platform.EUNE);
		assertThat(match.getSetNumber()).isEqualTo(18);
		assertThat(match.getPatch()).isEqualTo("18.1");
		assertThat(match.getCollectedAt()).isEqualTo(COLLECTED_AT);
		assertThat(match.getParticipants()).extracting(Participant::getPlacement).containsExactly(1, 8);

		Participant winner = match.getParticipants().getFirst();
		assertThat(winner.getLevel()).isEqualTo(9);
		assertThat(winner.getUnits()).hasSize(9);
		assertThat(winner.getUnits()).extracting(ParticipantUnit::getSlot).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8);
		assertThat(winner.getUnits().getFirst().getCharacterId()).isEqualTo("DA_18_Rakan");
	}

	@Test
	void shouldKeepOnlyActiveTraits_whenRiotListsInactiveOnes() {
		TftMatch match = MatchMapper.toEntity(RiotFixtures.realMatch(), RiotFixtures.PATCHES, COLLECTED_AT);

		Participant winner = match.getParticipants().getFirst();
		assertThat(winner.getTraits()).hasSize(10).allSatisfy(trait -> assertThat(trait.getTierCurrent()).isPositive());
	}

	@Test
	void shouldKeepItemOrder_whenUnitHoldsItems() {
		MatchDto dto = RiotFixtures.match("EUW1_1", 1100, 18, Instant.parse("2026-11-20T10:00:00Z"));

		TftMatch match = MatchMapper.toEntity(dto, RiotFixtures.PATCHES, COLLECTED_AT);

		ParticipantUnit unit = match.getParticipants().getFirst().getUnits().getFirst();
		assertThat(unit.getStarLevel()).isEqualTo(3);
		assertThat(unit.getItemNames()).containsExactly("DA_GuinsoosRageblade");
		assertThat(match.getPlatform()).isEqualTo(Platform.EUW);
		assertThat(match.getPatch()).isEqualTo("18.2");
	}

	@Test
	void shouldLeavePatchEmpty_whenGameIsOlderThanEveryKnownPatch() {
		MatchDto dto = RiotFixtures.match("EUW1_1", 1100, 18, Instant.parse("2026-10-01T10:00:00Z"));

		assertThat(MatchMapper.toEntity(dto, RiotFixtures.PATCHES, COLLECTED_AT).getPatch()).isNull();
	}

	@Test
	void shouldRejectMatch_whenPlatformPrefixIsUnknown() {
		MatchDto dto = RiotFixtures.match("NA1_1", 1100, 18, Instant.parse("2026-11-20T10:00:00Z"));

		assertThatThrownBy(() -> MatchMapper.toEntity(dto, RiotFixtures.PATCHES, COLLECTED_AT))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldCollectOnlyRankedGamesOfCurrentSet_whenCheckingMatches() {
		Instant end = Instant.parse("2026-11-20T10:00:00Z");

		assertThat(MatchMapper.isCollectable(RiotFixtures.match("EUW1_1", 1100, 18, end), 18)).isTrue();
		assertThat(MatchMapper.isCollectable(RiotFixtures.match("EUW1_1", 1090, 18, end), 18)).isFalse();
		assertThat(MatchMapper.isCollectable(RiotFixtures.match("EUW1_1", 1100, 17, end), 18)).isFalse();
	}
}
