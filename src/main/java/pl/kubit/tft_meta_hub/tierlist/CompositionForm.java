package pl.kubit.tft_meta_hub.tierlist;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.kubit.tft_meta_hub.tierlist.TierListDetails.CompositionView;
import pl.kubit.tft_meta_hub.tierlist.TierListDetails.ItemView;
import pl.kubit.tft_meta_hub.tierlist.TierListDetails.UnitView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Input for adding or editing a composition. The form always has {@value #ROWS} unit rows; rows without
 * a selected champion are ignored. Like {@link TierListForm}, it is a plain bean usable outside of Thymeleaf.
 */
@Getter
@Setter
@NoArgsConstructor
public class CompositionForm {

	public static final int ROWS = Composition.MAX_UNITS;
	public static final int ITEM_SLOTS = Composition.MAX_ITEMS_PER_UNIT;

	@NotBlank
	@Size(max = 100)
	private String name;

	@NotNull
	private Tier tier;

	@NotNull
	@Min(1)
	@Max(99)
	private Integer position;

	@NotNull
	private PlayStyle playStyle;

	@NotNull
	private Difficulty difficulty;

	@Size(max = Composition.MAX_TIPS_LENGTH)
	private String tips;

	private List<@Valid UnitForm> units = new ArrayList<>();

	public static CompositionForm empty() {
		CompositionForm form = new CompositionForm();
		form.position = 1;
		form.fillEmptyRows();
		return form;
	}

	public static CompositionForm from(CompositionView composition) {
		CompositionForm form = new CompositionForm();
		form.name = composition.name();
		form.tier = composition.tier();
		form.position = composition.position();
		form.playStyle = composition.playStyle();
		form.difficulty = composition.difficulty();
		form.tips = composition.tips();
		composition.units().forEach(unit -> form.units.add(UnitForm.from(unit)));
		form.fillEmptyRows();
		return form;
	}

	/** Pads the form to {@value #ROWS} rows with {@value #ITEM_SLOTS} item slots each, so the page can render all fields. */
	public void fillEmptyRows() {
		while (units.size() < ROWS) {
			units.add(new UnitForm());
		}
		units.forEach(UnitForm::fillEmptyItemSlots);
	}

	/** Rows where a champion was selected. */
	public List<UnitForm> selectedUnits() {
		return units.stream()
				.filter(unit -> unit.getChampionId() != null)
				.toList();
	}

	@Getter
	@Setter
	@NoArgsConstructor
	public static class UnitForm {

		private Long championId;

		private boolean carry;

		@NotNull
		@Min(1)
		@Max(3)
		private Integer starLevel = 2;

		private List<Long> itemIds = new ArrayList<>();

		static UnitForm from(UnitView unit) {
			UnitForm form = new UnitForm();
			form.championId = unit.championId();
			form.carry = unit.carry();
			form.starLevel = unit.starLevel();
			form.itemIds = new ArrayList<>(unit.items().stream().map(ItemView::id).toList());
			return form;
		}

		void fillEmptyItemSlots() {
			while (itemIds.size() < ITEM_SLOTS) {
				itemIds.add(null);
			}
		}

		/** Item slots where an item was selected. */
		public List<Long> selectedItemIds() {
			return itemIds.stream()
					.filter(Objects::nonNull)
					.toList();
		}
	}
}
