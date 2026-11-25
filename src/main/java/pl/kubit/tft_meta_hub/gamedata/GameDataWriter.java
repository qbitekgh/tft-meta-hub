package pl.kubit.tft_meta_hub.gamedata;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pl.kubit.tft_meta_hub.gamedata.GameData.ChampionData;
import pl.kubit.tft_meta_hub.gamedata.GameData.ItemData;
import pl.kubit.tft_meta_hub.gamedata.GameData.TraitData;
import pl.kubit.tft_meta_hub.gamedata.GameDataImportResult.ImportCounts;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Saves game data of one set idempotently: a record that already exists (same set number and api name)
 * is updated, a new one is inserted. Records missing from the new data are kept, because tier lists
 * may still refer to them.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class GameDataWriter {

	private final TraitRepository traitRepository;
	private final ChampionRepository championRepository;
	private final ItemRepository itemRepository;

	@Transactional
	public GameDataImportResult save(GameData gameData) {
		ImportCounts traits = saveTraits(gameData);
		ImportCounts champions = saveChampions(gameData);
		ImportCounts items = saveItems(gameData);
		return new GameDataImportResult(gameData.setNumber(), champions, traits, items);
	}

	private ImportCounts saveTraits(GameData gameData) {
		int setNumber = gameData.setNumber();
		Map<String, Trait> existing = byApiName(traitRepository.findAllBySetNumber(setNumber), Trait::getApiName);
		int added = 0;
		int updated = 0;
		for (TraitData data : gameData.traits()) {
			Trait trait = existing.get(data.apiName());
			if (trait == null) {
				traitRepository.save(Trait.create(setNumber, data));
				added++;
			} else {
				trait.updateFrom(data);
				updated++;
			}
		}
		return new ImportCounts(added, updated);
	}

	private ImportCounts saveChampions(GameData gameData) {
		int setNumber = gameData.setNumber();
		Map<String, Trait> traitsByName = traitsByName(setNumber);
		Map<String, Champion> existing = byApiName(championRepository.findAllBySetNumber(setNumber), Champion::getApiName);
		int added = 0;
		int updated = 0;
		for (ChampionData data : gameData.champions()) {
			Set<Trait> traits = findTraits(data, traitsByName);
			Champion champion = existing.get(data.apiName());
			if (champion == null) {
				championRepository.save(Champion.create(setNumber, data, traits));
				added++;
			} else {
				champion.updateFrom(data, traits);
				updated++;
			}
		}
		return new ImportCounts(added, updated);
	}

	private ImportCounts saveItems(GameData gameData) {
		int setNumber = gameData.setNumber();
		Map<String, Item> existing = byApiName(itemRepository.findAllBySetNumber(setNumber), Item::getApiName);
		int added = 0;
		int updated = 0;
		for (ItemData data : gameData.items()) {
			Item item = existing.get(data.apiName());
			if (item == null) {
				itemRepository.save(Item.create(setNumber, data));
				added++;
			} else {
				item.updateFrom(data);
				updated++;
			}
		}
		return new ImportCounts(added, updated);
	}

	/**
	 * Champions refer to traits by display name. A name is not always unique (in set 17 "Stargazer" has
	 * one trait per constellation), so the first trait with a given name wins.
	 */
	private Map<String, Trait> traitsByName(int setNumber) {
		return traitRepository.findAllBySetNumber(setNumber).stream()
				.collect(Collectors.toMap(Trait::getName, Function.identity(), (first, duplicate) -> {
					log.warn("Trait name '{}' is used by {} and {}; champions are linked to {}",
							first.getName(), first.getApiName(), duplicate.getApiName(), first.getApiName());
					return first;
				}));
	}

	private Set<Trait> findTraits(ChampionData champion, Map<String, Trait> traitsByName) {
		Set<Trait> traits = new HashSet<>();
		for (String traitName : champion.traitNames()) {
			Trait trait = traitsByName.get(traitName);
			if (trait == null) {
				log.warn("Champion {} refers to unknown trait '{}'", champion.apiName(), traitName);
			} else {
				traits.add(trait);
			}
		}
		return traits;
	}

	private static <T> Map<String, T> byApiName(List<T> entities, Function<T, String> apiName) {
		return entities.stream().collect(Collectors.toMap(apiName, Function.identity()));
	}
}
