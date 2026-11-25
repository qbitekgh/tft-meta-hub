package pl.kubit.tft_meta_hub.gamedata;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.kubit.tft_meta_hub.gamedata.GameData.ChampionData;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "champion")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Champion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private int setNumber;

	/** Identifier shared with Riot match data ({@code character_id}), e.g. {@code TFT17_Briar}. */
	private String apiName;

	private String name;

	private int cost;

	private String iconUrl;

	@ManyToMany
	@JoinTable(name = "champion_trait",
			joinColumns = @JoinColumn(name = "champion_id"),
			inverseJoinColumns = @JoinColumn(name = "trait_id"))
	private Set<Trait> traits = new HashSet<>();

	public static Champion create(int setNumber, ChampionData data, Set<Trait> traits) {
		Champion champion = new Champion();
		champion.setNumber = setNumber;
		champion.apiName = data.apiName();
		champion.updateFrom(data, traits);
		return champion;
	}

	public void updateFrom(ChampionData data, Set<Trait> traits) {
		this.name = data.name();
		this.cost = data.cost();
		this.iconUrl = data.iconUrl();
		this.traits.clear();
		this.traits.addAll(traits);
	}

	public Set<Trait> getTraits() {
		return Collections.unmodifiableSet(traits);
	}
}
