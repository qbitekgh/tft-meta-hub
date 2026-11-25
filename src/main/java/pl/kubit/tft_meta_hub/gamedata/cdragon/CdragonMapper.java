package pl.kubit.tft_meta_hub.gamedata.cdragon;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.kubit.tft_meta_hub.gamedata.GameData;
import pl.kubit.tft_meta_hub.gamedata.GameData.ChampionData;
import pl.kubit.tft_meta_hub.gamedata.GameData.ItemData;
import pl.kubit.tft_meta_hub.gamedata.GameData.TraitData;
import pl.kubit.tft_meta_hub.gamedata.ItemCategory;
import pl.kubit.tft_meta_hub.gamedata.SetNotFoundException;
import pl.kubit.tft_meta_hub.gamedata.cdragon.CdragonData.CdragonChampion;
import pl.kubit.tft_meta_hub.gamedata.cdragon.CdragonData.CdragonItem;
import pl.kubit.tft_meta_hub.gamedata.cdragon.CdragonData.CdragonSet;
import pl.kubit.tft_meta_hub.gamedata.cdragon.CdragonData.CdragonSetData;
import pl.kubit.tft_meta_hub.gamedata.cdragon.CdragonData.CdragonTrait;
import pl.kubit.tft_meta_hub.gamedata.cdragon.CdragonData.CdragonTraitEffect;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Selects one set from the Community Dragon file and converts it into {@link GameData}.
 */
@Component
@RequiredArgsConstructor
public class CdragonMapper {

	private static final int MIN_COST = 1;
	private static final int MAX_COST = 5;

	private final CdragonIconUrls iconUrls;

	public GameData map(CdragonData data, int setNumber) {
		CdragonSet set = data.sets().get(String.valueOf(setNumber));
		CdragonSetData rankedMode = findRankedMode(data, setNumber);
		if (set == null || rankedMode == null) {
			throw new SetNotFoundException(setNumber);
		}

		List<TraitData> traits = set.traits().stream()
				.map(this::toTraitData)
				.toList();

		List<ChampionData> champions = set.champions().stream()
				.filter(CdragonMapper::isPlayable)
				.map(this::toChampionData)
				.toList();

		Map<String, CdragonItem> itemsByApiName = data.items().stream()
				.collect(Collectors.toMap(CdragonItem::apiName, Function.identity(), (first, second) -> first));

		List<ItemData> items = rankedMode.items().stream()
				.map(itemsByApiName::get)
				.filter(Objects::nonNull)
				.flatMap(item -> ItemClassifier.classify(item)
						.map(category -> toItemData(item, category))
						.stream())
				.toList();

		return new GameData(setNumber, traits, champions, items);
	}

	/** The ranked mode of set N is called {@code TFTSet<N>}; other entries are Double Up, PvE modes and events. */
	private static CdragonSetData findRankedMode(CdragonData data, int setNumber) {
		String mutator = "TFTSet" + setNumber;
		return data.setData().stream()
				.filter(setData -> mutator.equals(setData.mutator()))
				.findFirst()
				.orElse(null);
	}

	/** Filters out summons, training dummies, PvE monsters and technical units (they have no traits or cost 8+). */
	private static boolean isPlayable(CdragonChampion champion) {
		return champion.cost() >= MIN_COST
				&& champion.cost() <= MAX_COST
				&& champion.traits() != null
				&& !champion.traits().isEmpty();
	}

	private TraitData toTraitData(CdragonTrait trait) {
		List<Integer> breakpoints = trait.effects().stream()
				.map(CdragonTraitEffect::minUnits)
				.filter(Objects::nonNull)
				.distinct()
				.sorted()
				.toList();
		return new TraitData(trait.apiName(), trait.name(), iconUrls.toUrl(trait.icon()), breakpoints);
	}

	private ChampionData toChampionData(CdragonChampion champion) {
		return new ChampionData(champion.apiName(), champion.name(), champion.cost(),
				iconUrls.toUrl(champion.tileIcon()), List.copyOf(champion.traits()));
	}

	private ItemData toItemData(CdragonItem item, ItemCategory category) {
		return new ItemData(item.apiName(), item.name(), iconUrls.toUrl(item.icon()), category);
	}
}
