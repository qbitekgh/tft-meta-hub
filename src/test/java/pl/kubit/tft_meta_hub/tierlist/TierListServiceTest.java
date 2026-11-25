package pl.kubit.tft_meta_hub.tierlist;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pl.kubit.tft_meta_hub.gamedata.Champion;
import pl.kubit.tft_meta_hub.gamedata.ChampionRepository;
import pl.kubit.tft_meta_hub.gamedata.Item;
import pl.kubit.tft_meta_hub.gamedata.ItemRepository;
import pl.kubit.tft_meta_hub.tierlist.CompositionForm.UnitForm;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static pl.kubit.tft_meta_hub.tierlist.TierListFixtures.LATER;
import static pl.kubit.tft_meta_hub.tierlist.TierListFixtures.champion;
import static pl.kubit.tft_meta_hub.tierlist.TierListFixtures.draft;
import static pl.kubit.tft_meta_hub.tierlist.TierListFixtures.item;

@ExtendWith(MockitoExtension.class)
class TierListServiceTest {

	@Mock
	private TierListRepository tierListRepository;

	@Mock
	private ChampionRepository championRepository;

	@Mock
	private ItemRepository itemRepository;

	private TierListService service;

	@BeforeEach
	void setUp() {
		Clock fixedClock = Clock.fixed(LATER, ZoneOffset.UTC);
		service = new TierListService(tierListRepository, championRepository, itemRepository, fixedClock);
	}

	@Test
	void shouldArchivePreviousAndFlushBeforePublishing_whenAnotherListIsPublished() {
		TierList previous = withId(draft(), 1L);
		previous.publish(TierListFixtures.CREATED_AT);
		TierList next = withId(draft(), 2L);
		given(tierListRepository.findById(2L)).willReturn(Optional.of(next));
		given(tierListRepository.findByStatus(TierListStatus.PUBLISHED)).willReturn(Optional.of(previous));

		service.publish(2L);

		assertThat(previous.getStatus()).isEqualTo(TierListStatus.ARCHIVED);
		assertThat(next.getStatus()).isEqualTo(TierListStatus.PUBLISHED);
		assertThat(next.getPublishedAt()).isEqualTo(LATER);
		then(tierListRepository).should().flush();
	}

	@Test
	void shouldPublishWithoutFlushing_whenNothingIsPublishedYet() {
		TierList tierList = withId(draft(), 1L);
		given(tierListRepository.findById(1L)).willReturn(Optional.of(tierList));
		given(tierListRepository.findByStatus(TierListStatus.PUBLISHED)).willReturn(Optional.empty());

		service.publish(1L);

		assertThat(tierList.getStatus()).isEqualTo(TierListStatus.PUBLISHED);
		then(tierListRepository).should(never()).flush();
	}

	@Test
	void shouldCreateDraftWithClockTime_whenGameDataOfSetExists() {
		given(championRepository.existsBySetNumber(18)).willReturn(true);
		given(tierListRepository.save(any(TierList.class))).willAnswer(invocation -> withId(invocation.getArgument(0), 7L));

		long id = service.createDraft(new TierListForm("Patch 18.1", "18.1", 18));

		assertThat(id).isEqualTo(7L);
		InOrder order = inOrder(championRepository, tierListRepository);
		order.verify(championRepository).existsBySetNumber(18);
		order.verify(tierListRepository).save(argThat(tierList ->
				tierList.getStatus() == TierListStatus.DRAFT && tierList.getUpdatedAt().equals(LATER)));
	}

	@Test
	void shouldRejectCreatingDraft_whenSetHasNoGameData() {
		given(championRepository.existsBySetNumber(19)).willReturn(false);

		assertThatThrownBy(() -> service.createDraft(new TierListForm("Set 19", "19.1", 19)))
				.isInstanceOf(TierListRuleException.class)
				.hasMessage("There is no game data for set 19. Import the set first.");
		then(tierListRepository).should(never()).save(any());
	}

	@Test
	void shouldThrowNotFound_whenTierListDoesNotExist() {
		given(tierListRepository.findById(99L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> service.getDetails(99L))
				.isInstanceOf(TierListNotFoundException.class)
				.hasMessage("Tier list 99 not found");
	}

	@Test
	void shouldRejectDeleting_whenTierListIsPublished() {
		TierList published = withId(draft(), 1L);
		published.publish(LATER);
		given(tierListRepository.findById(1L)).willReturn(Optional.of(published));

		assertThatThrownBy(() -> service.delete(1L))
				.isInstanceOf(TierListRuleException.class)
				.hasMessage("Only a draft can be deleted.");
		then(tierListRepository).should(never()).delete(any());
	}

	@Test
	void shouldBuildCompositionFromSelectedRowsOnly_whenAddingComposition() {
		TierList tierList = withId(draft(), 1L);
		Champion xayah = withId(champion("Xayah", 1), 10L);
		Champion ornn = withId(champion("Ornn", 1), 11L);
		Item guinsoo = withId(item("Guinsoo's Rageblade"), 20L);
		given(tierListRepository.findById(1L)).willReturn(Optional.of(tierList));
		given(championRepository.findAllById(List.of(10L, 11L))).willReturn(List.of(xayah, ornn));
		given(itemRepository.findAllById(List.of(20L))).willReturn(List.of(guinsoo));

		CompositionForm form = CompositionForm.empty();
		form.setName("Fae Xayah");
		form.setTier(Tier.S);
		form.setPlayStyle(PlayStyle.REROLL_1_COST);
		form.setDifficulty(Difficulty.EASY);
		fillRow(form.getUnits().get(0), 10L, true, 3, 20L, null, 20L);
		fillRow(form.getUnits().get(4), 11L, false, 2);

		service.addComposition(1L, form);

		Composition composition = tierList.getCompositions().getFirst();
		assertThat(composition.getPlayStyle()).isEqualTo(PlayStyle.REROLL_1_COST);
		assertThat(composition.getUnits()).hasSize(2);
		CompositionUnit carry = composition.getUnits().getFirst();
		assertThat(carry.getChampion()).isSameAs(xayah);
		assertThat(carry.isCarry()).isTrue();
		assertThat(carry.getStarLevel()).isEqualTo(3);
		assertThat(carry.getItems()).containsExactly(guinsoo, guinsoo);
		assertThat(composition.getUnits().get(1).getChampion()).isSameAs(ornn);
	}

	@Test
	void shouldRejectComposition_whenSelectedChampionDoesNotExist() {
		TierList tierList = withId(draft(), 1L);
		given(tierListRepository.findById(1L)).willReturn(Optional.of(tierList));
		given(championRepository.findAllById(List.of(404L))).willReturn(List.of());

		CompositionForm form = CompositionForm.empty();
		form.setName("Ghost");
		form.setTier(Tier.S);
		form.setPlayStyle(PlayStyle.FLEX);
		form.setDifficulty(Difficulty.HARD);
		fillRow(form.getUnits().getFirst(), 404L, false, 2);

		assertThatThrownBy(() -> service.addComposition(1L, form))
				.isInstanceOf(TierListRuleException.class)
				.hasMessageContaining("Unknown champion");
		assertThat(tierList.getCompositions()).isEmpty();
	}

	private static void fillRow(UnitForm row, Long championId, boolean carry, int stars, Long... itemIds) {
		row.setChampionId(championId);
		row.setCarry(carry);
		row.setStarLevel(stars);
		row.setItemIds(new ArrayList<>(Arrays.asList(itemIds)));
	}

	private static <T> T withId(T entity, long id) {
		ReflectionTestUtils.setField(entity, "id", id);
		return entity;
	}
}
