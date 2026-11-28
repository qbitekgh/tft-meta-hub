package pl.kubit.tft_meta_hub.gamedata;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.kubit.tft_meta_hub.gamedata.GameDataCatalog.ChampionInfo;
import pl.kubit.tft_meta_hub.gamedata.GameDataCatalog.ItemInfo;
import pl.kubit.tft_meta_hub.gamedata.GameDataCatalog.TraitInfo;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Gives other modules (the explorer) the imported game data of a set as plain records. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameDataCatalogService {

	private final ChampionRepository championRepository;
	private final TraitRepository traitRepository;
	private final ItemRepository itemRepository;

	public GameDataCatalog getCatalog(int setNumber) {
		List<ChampionInfo> champions = championRepository.findAllBySetNumber(setNumber).stream()
				.map(champion -> new ChampionInfo(champion.getApiName(), champion.getName(), champion.getCost(),
						champion.getIconUrl()))
				.toList();
		List<TraitInfo> traits = traitRepository.findAllBySetNumber(setNumber).stream()
				.map(trait -> new TraitInfo(trait.getApiName(), trait.getName(), trait.getIconUrl(),
						List.copyOf(trait.getBreakpoints())))
				.toList();
		// Generic twins are dropped: match data refers to the set-specific item, and filter lists show each name once
		List<ItemInfo> items = SetItems.withoutGenericDuplicates(itemRepository.findAllBySetNumber(setNumber), setNumber)
				.stream()
				.map(item -> new ItemInfo(item.getApiName(), item.getName(), item.getIconUrl(), item.getCategory()))
				.toList();
		return new GameDataCatalog(setNumber,
				byApiName(champions, ChampionInfo::apiName),
				byApiName(traits, TraitInfo::apiName),
				byApiName(items, ItemInfo::apiName));
	}

	private static <T> Map<String, T> byApiName(List<T> values, Function<T, String> apiName) {
		return values.stream().collect(Collectors.toMap(apiName, Function.identity()));
	}
}
