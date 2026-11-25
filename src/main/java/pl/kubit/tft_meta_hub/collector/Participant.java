package pl.kubit.tft_meta_hub.collector;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One player's final board in one match. Deliberately anonymous: only placement, level, units and traits. */
@Entity
@Table(name = "participant")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Participant {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "match_id")
	private TftMatch match;

	private int placement;

	private int level;

	@OneToMany(mappedBy = "participant", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("slot")
	private List<ParticipantUnit> units = new ArrayList<>();

	/** Only traits that were active at the end of the game. */
	@ElementCollection
	@CollectionTable(name = "participant_trait", joinColumns = @JoinColumn(name = "participant_id"))
	private List<TraitResult> traits = new ArrayList<>();

	static Participant create(TftMatch match, int placement, int level) {
		Participant participant = new Participant();
		participant.match = match;
		participant.placement = placement;
		participant.level = level;
		return participant;
	}

	void addUnit(String characterId, int starLevel, List<String> itemNames) {
		units.add(ParticipantUnit.create(this, units.size(), characterId, starLevel, itemNames));
	}

	void addTrait(TraitResult trait) {
		traits.add(trait);
	}

	public List<ParticipantUnit> getUnits() {
		return Collections.unmodifiableList(units);
	}

	public List<TraitResult> getTraits() {
		return Collections.unmodifiableList(traits);
	}
}
