package pl.kubit.tft_meta_hub.tierlist;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import pl.kubit.tft_meta_hub.TestcontainersConfiguration;
import pl.kubit.tft_meta_hub.gamedata.Champion;
import pl.kubit.tft_meta_hub.gamedata.ChampionRepository;
import pl.kubit.tft_meta_hub.gamedata.Item;
import pl.kubit.tft_meta_hub.gamedata.ItemRepository;
import pl.kubit.tft_meta_hub.tierlist.CompositionForm.UnitForm;
import pl.kubit.tft_meta_hub.tierlist.TierListDetails.CompositionView;
import pl.kubit.tft_meta_hub.tierlist.TierListDetails.ItemView;
import pl.kubit.tft_meta_hub.tierlist.TierListDetails.UnitView;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Tier list use cases against a real PostgreSQL: checks the JPA mappings, cascades, orphan removal
 * and the "single published tier list" unique index. Each test is rolled back.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class TierListServiceIntegrationTest {

	@Autowired
	private TierListService service;

	@Autowired
	private TierListRepository tierListRepository;

	@Autowired
	private ChampionRepository championRepository;

	@Autowired
	private ItemRepository itemRepository;

	@Autowired
	private EntityManager entityManager;

	private Champion xayah;
	private Champion ornn;
	private Item guinsoo;
	private Item emblem;

	@BeforeEach
	void setUp() {
		xayah = championRepository.save(TierListFixtures.champion("Xayah", 1));
		ornn = championRepository.save(TierListFixtures.champion("Ornn", 1));
		guinsoo = itemRepository.save(TierListFixtures.item("Guinsoo's Rageblade"));
		emblem = itemRepository.save(TierListFixtures.item("Fae Emblem"));
	}

	@Test
	void shouldStoreCompositionWithOrderedUnitsAndItems_whenCompositionIsAdded() {
		long tierListId = service.createDraft(new TierListForm("Patch 18.1", "18.1", 18));

		service.addComposition(tierListId, form("Fae Xayah", Tier.S,
				row(xayah.getId(), true, 3, guinsoo.getId(), emblem.getId(), guinsoo.getId()),
				row(ornn.getId(), false, 2)));
		flushAndClear();

		CompositionView composition = service.getDetails(tierListId).compositions().getFirst();
		assertThat(composition.name()).isEqualTo("Fae Xayah");
		assertThat(composition.units())
				.extracting(UnitView::championName, UnitView::carry, UnitView::starLevel)
				.containsExactly(tuple("Xayah", true, 3), tuple("Ornn", false, 2));
		assertThat(composition.units().getFirst().items())
				.extracting(ItemView::name)
				.containsExactly("Guinsoo's Rageblade", "Fae Emblem", "Guinsoo's Rageblade");
	}

	@Test
	void shouldReplaceUnits_whenCompositionIsUpdated() {
		long tierListId = service.createDraft(new TierListForm("Patch 18.1", "18.1", 18));
		service.addComposition(tierListId, form("Draft comp", Tier.B, row(xayah.getId(), false, 2), row(ornn.getId(), false, 2)));
		flushAndClear();
		long compositionId = service.getDetails(tierListId).compositions().getFirst().id();

		service.updateComposition(tierListId, compositionId, form("Final comp", Tier.A, row(ornn.getId(), true, 3, emblem.getId())));
		flushAndClear();

		CompositionView updated = service.getDetails(tierListId).compositions().getFirst();
		assertThat(updated.name()).isEqualTo("Final comp");
		assertThat(updated.tier()).isEqualTo(Tier.A);
		assertThat(updated.units()).extracting(UnitView::championName).containsExactly("Ornn");
		assertThat(countRows("composition_unit")).isEqualTo(1);
	}

	@Test
	void shouldDeleteCompositionWithUnits_whenCompositionIsRemoved() {
		long tierListId = service.createDraft(new TierListForm("Patch 18.1", "18.1", 18));
		service.addComposition(tierListId, form("Comp", Tier.S, row(xayah.getId(), false, 2, guinsoo.getId())));
		flushAndClear();
		long compositionId = service.getDetails(tierListId).compositions().getFirst().id();

		service.deleteComposition(tierListId, compositionId);
		flushAndClear();

		assertThat(service.getDetails(tierListId).compositions()).isEmpty();
		assertThat(countRows("composition_unit")).isZero();
		assertThat(countRows("composition_unit_item")).isZero();
	}

	@Test
	void shouldKeepSinglePublishedList_whenAnotherDraftIsPublished() {
		long first = service.createDraft(new TierListForm("Patch 18.1", "18.1", 18));
		long second = service.createDraft(new TierListForm("Patch 18.2", "18.2", 18));

		service.publish(first);
		service.publish(second);
		flushAndClear();

		assertThat(service.getDetails(first).status()).isEqualTo(TierListStatus.ARCHIVED);
		assertThat(service.getDetails(second).status()).isEqualTo(TierListStatus.PUBLISHED);
	}

	@Test
	void shouldRejectSecondPublishedRow_whenServiceRuleIsBypassed() {
		Instant now = Instant.parse("2026-11-25T12:00:00Z");
		TierList first = TierList.createDraft("First", "18.1", 18, now);
		TierList second = TierList.createDraft("Second", "18.2", 18, now);
		first.publish(now);
		second.publish(now);

		assertThatThrownBy(() -> tierListRepository.saveAllAndFlush(List.of(first, second)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void shouldCopyCompositionsToNewDraft_whenCopiedToNewPatch() {
		long originalId = service.createDraft(new TierListForm("Patch 18.1", "18.1", 18));
		service.addComposition(originalId, form("Fae Xayah", Tier.S, row(xayah.getId(), true, 2, guinsoo.getId())));
		service.publish(originalId);
		flushAndClear();

		long copyId = service.copyToNewPatch(originalId, new TierListForm("Patch 18.2", "18.2", 18));
		flushAndClear();

		TierListDetails copy = service.getDetails(copyId);
		assertThat(copy.status()).isEqualTo(TierListStatus.DRAFT);
		assertThat(copy.patch()).isEqualTo("18.2");
		assertThat(copy.compositions()).extracting(CompositionView::name).containsExactly("Fae Xayah");
		assertThat(copy.compositions().getFirst().id())
				.isNotEqualTo(service.getDetails(originalId).compositions().getFirst().id());
		assertThat(copy.compositions().getFirst().units().getFirst().items())
				.extracting(ItemView::name).containsExactly("Guinsoo's Rageblade");
	}

	@Test
	void shouldDeleteDraftWithCompositions_whenDraftIsDeleted() {
		long tierListId = service.createDraft(new TierListForm("Patch 18.1", "18.1", 18));
		service.addComposition(tierListId, form("Comp", Tier.S, row(xayah.getId(), false, 2, guinsoo.getId())));
		flushAndClear();

		service.delete(tierListId);
		flushAndClear();

		assertThat(tierListRepository.findById(tierListId)).isEmpty();
		assertThat(countRows("composition")).isZero();
		assertThat(countRows("composition_unit_item")).isZero();
	}

	private static CompositionForm form(String name, Tier tier, UnitForm... rows) {
		CompositionForm form = CompositionForm.empty();
		form.setName(name);
		form.setTier(tier);
		form.setPlayStyle(PlayStyle.FLEX);
		form.setDifficulty(Difficulty.MEDIUM);
		for (int i = 0; i < rows.length; i++) {
			form.getUnits().set(i, rows[i]);
		}
		return form;
	}

	private static UnitForm row(Long championId, boolean carry, int stars, Long... itemIds) {
		UnitForm row = new UnitForm();
		row.setChampionId(championId);
		row.setCarry(carry);
		row.setStarLevel(stars);
		row.setItemIds(new ArrayList<>(Arrays.asList(itemIds)));
		return row;
	}

	/** Writes pending changes and empties the persistence context, so the next read really comes from the database. */
	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}

	private long countRows(String table) {
		return ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM " + table).getSingleResult()).longValue();
	}
}
