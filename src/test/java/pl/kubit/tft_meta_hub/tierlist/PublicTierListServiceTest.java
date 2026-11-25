package pl.kubit.tft_meta_hub.tierlist;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pl.kubit.tft_meta_hub.gamedata.Champion;
import pl.kubit.tft_meta_hub.tierlist.TierListDetails.CompositionView;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static pl.kubit.tft_meta_hub.tierlist.TierListFixtures.LATER;
import static pl.kubit.tft_meta_hub.tierlist.TierListFixtures.champion;
import static pl.kubit.tft_meta_hub.tierlist.TierListFixtures.composition;
import static pl.kubit.tft_meta_hub.tierlist.TierListFixtures.draft;
import static pl.kubit.tft_meta_hub.tierlist.TierListFixtures.unit;

@ExtendWith(MockitoExtension.class)
class PublicTierListServiceTest {

	@Mock
	private TierListRepository tierListRepository;

	private PublicTierListService service;

	@BeforeEach
	void setUp() {
		service = new PublicTierListService(tierListRepository);
	}

	@Test
	void shouldGroupByTierInTierOrderKeepingPositions_whenCompositionsAreMixed() {
		List<CompositionView> compositions = List.of(
				view(1, "S1", Tier.S), view(2, "S2", Tier.S), view(3, "A1", Tier.A),
				view(4, "Situational", Tier.SITUATIONAL), view(5, "C1", Tier.C));

		Map<Tier, List<CompositionView>> grouped = PublicTierListService.groupByTier(compositions);

		assertThat(grouped.keySet()).containsExactly(Tier.S, Tier.A, Tier.C, Tier.SITUATIONAL);
		assertThat(grouped.get(Tier.S)).extracting(CompositionView::name).containsExactly("S1", "S2");
		assertThat(grouped).doesNotContainKey(Tier.B);
	}

	@Test
	void shouldReturnEmpty_whenNothingIsPublished() {
		given(tierListRepository.findByStatus(TierListStatus.PUBLISHED)).willReturn(Optional.empty());

		assertThat(service.findPublished()).isEmpty();
	}

	@Test
	void shouldReturnPublishedListGroupedByTier_whenListIsPublished() {
		given(tierListRepository.findByStatus(TierListStatus.PUBLISHED)).willReturn(Optional.of(publishedTierList()));

		PublishedTierList published = service.findPublished().orElseThrow();

		assertThat(published.title()).isEqualTo("Patch 18.1 tier list");
		assertThat(published.publishedAt()).isEqualTo(LATER);
		assertThat(published.compositionsByTier().keySet()).containsExactly(Tier.S, Tier.B);
		assertThat(published.compositionsByTier().get(Tier.S))
				.extracting(CompositionView::name)
				.containsExactly("Fae Xayah", "Ornn Tank");
	}

	@Test
	void shouldReturnComposition_whenItBelongsToPublishedList() {
		given(tierListRepository.findByStatus(TierListStatus.PUBLISHED)).willReturn(Optional.of(publishedTierList()));

		PublishedComposition published = service.getComposition(102L);

		assertThat(published.composition().name()).isEqualTo("Ornn Tank");
		assertThat(published.patch()).isEqualTo("18.1");
	}

	@Test
	void shouldThrowNotFound_whenCompositionIsNotInPublishedList() {
		given(tierListRepository.findByStatus(TierListStatus.PUBLISHED)).willReturn(Optional.of(publishedTierList()));

		assertThatThrownBy(() -> service.getComposition(999L))
				.isInstanceOf(TierListNotFoundException.class);
	}

	@Test
	void shouldThrowNotFound_whenNothingIsPublished() {
		given(tierListRepository.findByStatus(TierListStatus.PUBLISHED)).willReturn(Optional.empty());

		assertThatThrownBy(() -> service.getComposition(101L))
				.isInstanceOf(TierListNotFoundException.class);
	}

	/** A published list: "Fae Xayah" (S, id 101), "Ornn Tank" (S, id 102), "Bench comp" (B, id 103); ids set as the database would. */
	private static TierList publishedTierList() {
		Champion xayah = withId(champion("Xayah", 1), 10L);
		TierList tierList = withId(draft(), 1L);
		tierList.addComposition(composition("Ornn Tank", Tier.S, 2, List.of(unit(xayah))), LATER);
		tierList.addComposition(composition("Bench comp", Tier.B, 1, List.of(unit(xayah))), LATER);
		tierList.addComposition(composition("Fae Xayah", Tier.S, 1, List.of(unit(xayah))), LATER);
		for (Composition composition : tierList.getCompositions()) {
			long id = switch (composition.getName()) {
				case "Fae Xayah" -> 101L;
				case "Ornn Tank" -> 102L;
				default -> 103L;
			};
			withId(composition, id);
		}
		tierList.publish(LATER);
		return tierList;
	}

	private static CompositionView view(long id, String name, Tier tier) {
		return new CompositionView(id, name, tier, 1, PlayStyle.FLEX, Difficulty.EASY, null, List.of());
	}

	private static <T> T withId(T entity, long id) {
		ReflectionTestUtils.setField(entity, "id", id);
		return entity;
	}
}
