package pl.kubit.tft_meta_hub.tierlist;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.kubit.tft_meta_hub.gamedata.Champion;
import pl.kubit.tft_meta_hub.gamedata.Item;
import pl.kubit.tft_meta_hub.tierlist.CompositionContent.UnitContent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One champion on the final board of a composition. Created only through {@link Composition}. */
@Entity
@Table(name = "composition_unit")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompositionUnit {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "composition_id")
	private Composition composition;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "champion_id")
	private Champion champion;

	/** Position of the unit in the composition (0 = first). */
	private int slot;

	private boolean carry;

	private int starLevel;

	/** The same item may appear twice (e.g. two Guinsoo's Rageblades), so this is an ordered list, not a set. */
	@ManyToMany
	@JoinTable(name = "composition_unit_item",
			joinColumns = @JoinColumn(name = "unit_id"),
			inverseJoinColumns = @JoinColumn(name = "item_id"))
	@OrderColumn(name = "sort_order")
	private List<Item> items = new ArrayList<>();

	static CompositionUnit create(Composition composition, int slot, UnitContent content) {
		CompositionUnit unit = new CompositionUnit();
		unit.composition = composition;
		unit.slot = slot;
		unit.champion = content.champion();
		unit.carry = content.carry();
		unit.starLevel = content.starLevel();
		unit.items.addAll(content.items());
		return unit;
	}

	UnitContent toContent() {
		return new UnitContent(champion, carry, starLevel, List.copyOf(items));
	}

	public List<Item> getItems() {
		return Collections.unmodifiableList(items);
	}
}
