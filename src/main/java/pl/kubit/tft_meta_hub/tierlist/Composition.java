package pl.kubit.tft_meta_hub.tierlist;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.kubit.tft_meta_hub.tierlist.CompositionContent.UnitContent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A team composition (final board) placed in a tier of a {@link TierList}.
 * Created and changed only through its tier list, which checks the rules first.
 */
@Entity
@Table(name = "composition")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Composition {

	static final int MIN_UNITS = 1;
	static final int MAX_UNITS = 10;
	static final int MIN_STAR_LEVEL = 1;
	static final int MAX_STAR_LEVEL = 3;
	static final int MAX_ITEMS_PER_UNIT = 3;
	static final int MAX_TIPS_LENGTH = 2000;

	/** Display order inside a tier list: by tier (S first), then by position inside the tier. */
	static final Comparator<Composition> DISPLAY_ORDER = Comparator
			.comparing(Composition::getTier)
			.thenComparingInt(Composition::getPosition);

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "tier_list_id")
	private TierList tierList;

	private String name;

	@Enumerated(EnumType.STRING)
	private Tier tier;

	/** Position inside the tier, starting at 1. */
	private int position;

	@Enumerated(EnumType.STRING)
	private PlayStyle playStyle;

	@Enumerated(EnumType.STRING)
	private Difficulty difficulty;

	private String tips;

	@OneToMany(mappedBy = "composition", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("slot")
	private List<CompositionUnit> units = new ArrayList<>();

	static Composition create(TierList tierList, CompositionContent content) {
		Composition composition = new Composition();
		composition.tierList = tierList;
		composition.update(content);
		return composition;
	}

	void update(CompositionContent content) {
		validate(content, tierList.getSetNumber());
		this.name = content.name().strip();
		this.tier = content.tier();
		this.position = content.position();
		this.playStyle = content.playStyle();
		this.difficulty = content.difficulty();
		this.tips = content.tips() == null || content.tips().isBlank() ? null : content.tips().strip();
		this.units.clear();
		for (int slot = 0; slot < content.units().size(); slot++) {
			this.units.add(CompositionUnit.create(this, slot, content.units().get(slot)));
		}
	}

	CompositionContent toContent() {
		List<UnitContent> unitContents = units.stream()
				.map(CompositionUnit::toContent)
				.toList();
		return new CompositionContent(name, tier, position, playStyle, difficulty, tips, unitContents);
	}

	public List<CompositionUnit> getUnits() {
		return Collections.unmodifiableList(units);
	}

	private static void validate(CompositionContent content, int setNumber) {
		if (content.name() == null || content.name().isBlank()) {
			throw new TierListRuleException("Composition name must not be empty.");
		}
		if (content.tier() == null || content.playStyle() == null || content.difficulty() == null) {
			throw new TierListRuleException("Tier, play style and difficulty are required.");
		}
		if (content.position() < 1) {
			throw new TierListRuleException("Position in tier must be 1 or higher.");
		}
		if (content.tips() != null && content.tips().length() > MAX_TIPS_LENGTH) {
			throw new TierListRuleException("Tips must have at most " + MAX_TIPS_LENGTH + " characters.");
		}
		int unitCount = content.units().size();
		if (unitCount < MIN_UNITS || unitCount > MAX_UNITS) {
			throw new TierListRuleException(
					"A composition must have between " + MIN_UNITS + " and " + MAX_UNITS + " units, but has " + unitCount + ".");
		}
		content.units().forEach(unit -> validateUnit(unit, setNumber));
	}

	private static void validateUnit(UnitContent unit, int setNumber) {
		String championName = unit.champion().getName();
		if (unit.champion().getSetNumber() != setNumber) {
			throw new TierListRuleException(championName + " does not belong to set " + setNumber + ".");
		}
		if (unit.starLevel() < MIN_STAR_LEVEL || unit.starLevel() > MAX_STAR_LEVEL) {
			throw new TierListRuleException(championName + ": star level must be between 1 and 3.");
		}
		if (unit.items().size() > MAX_ITEMS_PER_UNIT) {
			throw new TierListRuleException(championName + " can hold at most " + MAX_ITEMS_PER_UNIT + " items.");
		}
		unit.items().stream()
				.filter(item -> item.getSetNumber() != setNumber)
				.findFirst()
				.ifPresent(item -> {
					throw new TierListRuleException(item.getName() + " does not belong to set " + setNumber + ".");
				});
	}
}
