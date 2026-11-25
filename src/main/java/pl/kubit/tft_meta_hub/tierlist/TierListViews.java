package pl.kubit.tft_meta_hub.tierlist;

import pl.kubit.tft_meta_hub.gamedata.Item;
import pl.kubit.tft_meta_hub.tierlist.TierListDetails.CompositionView;
import pl.kubit.tft_meta_hub.tierlist.TierListDetails.ItemView;
import pl.kubit.tft_meta_hub.tierlist.TierListDetails.UnitView;

/** Converts tier list entities into read-only records for views. */
final class TierListViews {

	private TierListViews() {
	}

	static TierListSummary toSummary(TierList tierList) {
		return new TierListSummary(tierList.getId(), tierList.getTitle(), tierList.getPatch(), tierList.getSetNumber(),
				tierList.getStatus(), tierList.getPublishedAt(), tierList.getUpdatedAt(), tierList.getCompositions().size());
	}

	static TierListDetails toDetails(TierList tierList) {
		return new TierListDetails(tierList.getId(), tierList.getTitle(), tierList.getPatch(), tierList.getSetNumber(),
				tierList.getStatus(), tierList.getPublishedAt(), tierList.getUpdatedAt(),
				tierList.isEditable(), tierList.isDeletable(),
				tierList.getCompositions().stream().map(TierListViews::toView).toList());
	}

	private static CompositionView toView(Composition composition) {
		return new CompositionView(composition.getId(), composition.getName(), composition.getTier(),
				composition.getPosition(), composition.getPlayStyle(), composition.getDifficulty(), composition.getTips(),
				composition.getUnits().stream().map(TierListViews::toView).toList());
	}

	private static UnitView toView(CompositionUnit unit) {
		return new UnitView(unit.getChampion().getId(), unit.getChampion().getName(), unit.getChampion().getIconUrl(),
				unit.getChampion().getCost(), unit.isCarry(), unit.getStarLevel(),
				unit.getItems().stream().map(TierListViews::toView).toList());
	}

	private static ItemView toView(Item item) {
		return new ItemView(item.getId(), item.getName(), item.getIconUrl());
	}
}
