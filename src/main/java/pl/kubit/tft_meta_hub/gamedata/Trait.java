package pl.kubit.tft_meta_hub.gamedata;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.kubit.tft_meta_hub.gamedata.GameData.TraitData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "trait")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trait {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private int setNumber;

	private String apiName;

	private String name;

	private String iconUrl;

	/** Minimum unit counts that activate the next trait level, e.g. [2, 4, 6]. */
	@ElementCollection
	@CollectionTable(name = "trait_breakpoint", joinColumns = @JoinColumn(name = "trait_id"))
	@OrderColumn(name = "sort_order")
	@Column(name = "min_units")
	private List<Integer> breakpoints = new ArrayList<>();

	public static Trait create(int setNumber, TraitData data) {
		Trait trait = new Trait();
		trait.setNumber = setNumber;
		trait.apiName = data.apiName();
		trait.updateFrom(data);
		return trait;
	}

	public void updateFrom(TraitData data) {
		this.name = data.name();
		this.iconUrl = data.iconUrl();
		this.breakpoints.clear();
		this.breakpoints.addAll(data.breakpoints());
	}

	public List<Integer> getBreakpoints() {
		return Collections.unmodifiableList(breakpoints);
	}
}
