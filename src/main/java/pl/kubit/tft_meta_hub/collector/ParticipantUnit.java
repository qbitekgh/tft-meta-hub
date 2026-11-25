package pl.kubit.tft_meta_hub.collector;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A unit on a board. Champion and items are stored as Riot's api names (e.g. "DA_18_Rakan", "DA_GuinsoosRageblade"),
 * not as foreign keys: matches may contain units or items that are not in our game data.
 */
@Entity
@Table(name = "participant_unit")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ParticipantUnit {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "participant_id")
	private Participant participant;

	private int slot;

	private String characterId;

	private int starLevel;

	@ElementCollection
	@CollectionTable(name = "participant_unit_item", joinColumns = @JoinColumn(name = "unit_id"))
	@OrderColumn(name = "slot")
	@Column(name = "item_name")
	private List<String> itemNames = new ArrayList<>();

	static ParticipantUnit create(Participant participant, int slot, String characterId, int starLevel, List<String> itemNames) {
		ParticipantUnit unit = new ParticipantUnit();
		unit.participant = participant;
		unit.slot = slot;
		unit.characterId = characterId;
		unit.starLevel = starLevel;
		unit.itemNames.addAll(itemNames);
		return unit;
	}

	public List<String> getItemNames() {
		return Collections.unmodifiableList(itemNames);
	}
}
