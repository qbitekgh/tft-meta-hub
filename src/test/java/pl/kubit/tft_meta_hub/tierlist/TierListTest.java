package pl.kubit.tft_meta_hub.tierlist;

import org.junit.jupiter.api.Test;
import pl.kubit.tft_meta_hub.gamedata.Champion;
import pl.kubit.tft_meta_hub.gamedata.Item;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static pl.kubit.tft_meta_hub.tierlist.TierListFixtures.CREATED_AT;
import static pl.kubit.tft_meta_hub.tierlist.TierListFixtures.LATER;
import static pl.kubit.tft_meta_hub.tierlist.TierListFixtures.champion;
import static pl.kubit.tft_meta_hub.tierlist.TierListFixtures.composition;
import static pl.kubit.tft_meta_hub.tierlist.TierListFixtures.draft;
import static pl.kubit.tft_meta_hub.tierlist.TierListFixtures.item;
import static pl.kubit.tft_meta_hub.tierlist.TierListFixtures.unit;
import static pl.kubit.tft_meta_hub.tierlist.TierListFixtures.units;

class TierListTest {

	@Test
	void shouldCreateEditableDraft_whenTierListIsCreated() {
		TierList tierList = TierList.createDraft("  Patch 18.1  ", " 18.1 ", 18, CREATED_AT);

		assertThat(tierList.getStatus()).isEqualTo(TierListStatus.DRAFT);
		assertThat(tierList.getTitle()).isEqualTo("Patch 18.1");
		assertThat(tierList.getPatch()).isEqualTo("18.1");
		assertThat(tierList.getUpdatedAt()).isEqualTo(CREATED_AT);
		assertThat(tierList.getPublishedAt()).isNull();
		assertThat(tierList.isEditable()).isTrue();
		assertThat(tierList.isDeletable()).isTrue();
	}

	@Test
	void shouldSetStatusAndPublishTime_whenDraftIsPublished() {
		TierList tierList = draft();

		tierList.publish(LATER);

		assertThat(tierList.getStatus()).isEqualTo(TierListStatus.PUBLISHED);
		assertThat(tierList.getPublishedAt()).isEqualTo(LATER);
		assertThat(tierList.getUpdatedAt()).isEqualTo(LATER);
		assertThat(tierList.isEditable()).isTrue();
		assertThat(tierList.isDeletable()).isFalse();
	}

	@Test
	void shouldRejectPublishing_whenTierListIsNotDraft() {
		TierList tierList = draft();
		tierList.publish(LATER);

		assertThatThrownBy(() -> tierList.publish(LATER))
				.isInstanceOf(TierListRuleException.class)
				.hasMessage("Only a draft can be published.");
	}

	@Test
	void shouldArchive_whenTierListIsPublished() {
		TierList tierList = draft();
		tierList.publish(CREATED_AT);

		tierList.archive(LATER);

		assertThat(tierList.getStatus()).isEqualTo(TierListStatus.ARCHIVED);
		assertThat(tierList.isEditable()).isFalse();
		assertThat(tierList.isDeletable()).isFalse();
	}

	@Test
	void shouldRejectArchiving_whenTierListIsDraft() {
		assertThatThrownBy(() -> draft().archive(LATER))
				.isInstanceOf(TierListRuleException.class);
	}

	@Test
	void shouldRejectAnyChange_whenTierListIsArchived() {
		TierList tierList = draft();
		tierList.publish(CREATED_AT);
		tierList.archive(CREATED_AT);

		assertThatThrownBy(() -> tierList.addComposition(composition("Comp", Tier.S, 1), LATER))
				.isInstanceOf(TierListRuleException.class)
				.hasMessageContaining("archived");
		assertThatThrownBy(() -> tierList.updateDetails("New title", "18.2", LATER))
				.isInstanceOf(TierListRuleException.class);
		assertThat(tierList.getCompositions()).isEmpty();
		assertThat(tierList.getTitle()).isEqualTo("Patch 18.1 tier list");
	}

	@Test
	void shouldAddCompositionWithUnitsInOrder_whenContentIsValid() {
		TierList tierList = draft();
		Item guinsoo = item("Guinsoo's Rageblade");

		tierList.addComposition(composition("Fae Xayah", Tier.A, 1,
				List.of(unit(champion("Xayah", 1), guinsoo, guinsoo), unit(champion("Ornn", 1)))), LATER);

		Composition added = tierList.getCompositions().getFirst();
		assertThat(added.getName()).isEqualTo("Fae Xayah");
		assertThat(added.getUnits())
				.extracting(unit -> unit.getChampion().getName(), CompositionUnit::getSlot)
				.containsExactly(tuple("Xayah", 0), tuple("Ornn", 1));
		assertThat(added.getUnits().getFirst().getItems()).containsExactly(guinsoo, guinsoo);
		assertThat(tierList.getUpdatedAt()).isEqualTo(LATER);
	}

	@Test
	void shouldRejectComposition_whenItHasNoUnits() {
		assertThatThrownBy(() -> draft().addComposition(composition("Empty", Tier.S, 1, List.of()), LATER))
				.isInstanceOf(TierListRuleException.class)
				.hasMessage("A composition must have between 1 and 10 units, but has 0.");
	}

	@Test
	void shouldRejectComposition_whenItHasMoreThanTenUnits() {
		assertThatThrownBy(() -> draft().addComposition(composition("Too big", Tier.S, 1, units(11)), LATER))
				.isInstanceOf(TierListRuleException.class)
				.hasMessage("A composition must have between 1 and 10 units, but has 11.");
	}

	@Test
	void shouldAcceptComposition_whenItHasExactlyTenUnits() {
		TierList tierList = draft();

		tierList.addComposition(composition("Level 10", Tier.S, 1, units(10)), LATER);

		assertThat(tierList.getCompositions().getFirst().getUnits()).hasSize(10);
	}

	@Test
	void shouldRejectComposition_whenUnitHoldsMoreThanThreeItems() {
		Item item = item("Infinity Edge");
		CompositionContent content = composition("Comp", Tier.S, 1, List.of(unit(champion("Xayah", 1), item, item, item, item)));

		assertThatThrownBy(() -> draft().addComposition(content, LATER))
				.isInstanceOf(TierListRuleException.class)
				.hasMessage("Xayah can hold at most 3 items.");
	}

	@Test
	void shouldRejectComposition_whenStarLevelIsOutOfRange() {
		CompositionContent content = composition("Comp", Tier.S, 1,
				List.of(new CompositionContent.UnitContent(champion("Xayah", 1), true, 4, List.of())));

		assertThatThrownBy(() -> draft().addComposition(content, LATER))
				.isInstanceOf(TierListRuleException.class)
				.hasMessage("Xayah: star level must be between 1 and 3.");
	}

	@Test
	void shouldRejectComposition_whenChampionOrItemIsFromAnotherSet() {
		Champion oldChampion = champion("Briar", 1, 17);
		Item oldItem = item("Old Item", 17);

		assertThatThrownBy(() -> draft().addComposition(composition("Comp", Tier.S, 1, List.of(unit(oldChampion))), LATER))
				.isInstanceOf(TierListRuleException.class)
				.hasMessage("Briar does not belong to set 18.");
		assertThatThrownBy(() -> draft().addComposition(
				composition("Comp", Tier.S, 1, List.of(unit(champion("Xayah", 1), oldItem))), LATER))
				.isInstanceOf(TierListRuleException.class)
				.hasMessage("Old Item does not belong to set 18.");
	}

	@Test
	void shouldReturnCompositionsByTierThenPosition_whenAddedInRandomOrder() {
		TierList tierList = draft();
		tierList.addComposition(composition("B1", Tier.B, 1), LATER);
		tierList.addComposition(composition("S2", Tier.S, 2), LATER);
		tierList.addComposition(composition("Situational", Tier.SITUATIONAL, 1), LATER);
		tierList.addComposition(composition("S1", Tier.S, 1), LATER);

		assertThat(tierList.getCompositions())
				.extracting(Composition::getName)
				.containsExactly("S1", "S2", "B1", "Situational");
	}

	@Test
	void shouldCopyAllCompositionsIntoNewDraft_whenCopiedToNewPatch() {
		TierList original = draft();
		Item guinsoo = item("Guinsoo's Rageblade");
		original.addComposition(composition("Fae Xayah", Tier.A, 1, List.of(unit(champion("Xayah", 1), guinsoo))), CREATED_AT);
		original.addComposition(composition("Ornn Tank", Tier.S, 1), CREATED_AT);
		original.publish(CREATED_AT);

		TierList copy = original.copyAsDraft("Patch 18.2 tier list", "18.2", LATER);

		assertThat(copy.getStatus()).isEqualTo(TierListStatus.DRAFT);
		assertThat(copy.getPatch()).isEqualTo("18.2");
		assertThat(copy.getSetNumber()).isEqualTo(18);
		assertThat(copy.getUpdatedAt()).isEqualTo(LATER);
		assertThat(copy.getCompositions())
				.extracting(Composition::getName, Composition::getTier)
				.containsExactly(tuple("Ornn Tank", Tier.S), tuple("Fae Xayah", Tier.A));
		Composition copiedComposition = copy.getCompositions().get(1);
		assertThat(copiedComposition).isNotSameAs(original.getCompositions().get(1));
		assertThat(copiedComposition.getTierList()).isSameAs(copy);
		assertThat(copiedComposition.getUnits().getFirst().getItems()).containsExactly(guinsoo);
		assertThat(original.getStatus()).isEqualTo(TierListStatus.PUBLISHED);
	}

	@Test
	void shouldAllowCopying_whenTierListIsArchived() {
		TierList archived = draft();
		archived.publish(CREATED_AT);
		archived.archive(CREATED_AT);

		TierList copy = archived.copyAsDraft("Revived", "18.3", LATER);

		assertThat(copy.isEditable()).isTrue();
	}
}
