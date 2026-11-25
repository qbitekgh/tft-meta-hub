package pl.kubit.tft_meta_hub.tierlist;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.kubit.tft_meta_hub.gamedata.Champion;
import pl.kubit.tft_meta_hub.gamedata.ChampionRepository;
import pl.kubit.tft_meta_hub.gamedata.Item;
import pl.kubit.tft_meta_hub.gamedata.ItemRepository;
import pl.kubit.tft_meta_hub.tierlist.CompositionContent.UnitContent;
import pl.kubit.tft_meta_hub.tierlist.CompositionForm.UnitForm;
import pl.kubit.tft_meta_hub.tierlist.CompositionFormOptions.ChampionOption;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Use cases of the admin's tier list management. Returns records only, so the same service can later
 * back a REST API. Business rules live in the {@link TierList} aggregate; this class loads data,
 * calls the aggregate and handles the one rule that spans several tier lists (a single published list).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TierListService {

	private final TierListRepository tierListRepository;
	private final ChampionRepository championRepository;
	private final ItemRepository itemRepository;
	private final Clock clock;

	@Transactional(readOnly = true)
	public List<TierListSummary> findAll() {
		return tierListRepository.findAllByOrderByUpdatedAtDesc().stream()
				.map(TierListViews::toSummary)
				.toList();
	}

	@Transactional(readOnly = true)
	public TierListDetails getDetails(long tierListId) {
		return TierListViews.toDetails(getTierList(tierListId));
	}

	public long createDraft(TierListForm form) {
		requireGameData(form.getSetNumber());
		TierList tierList = TierList.createDraft(form.getTitle(), form.getPatch(), form.getSetNumber(), now());
		return tierListRepository.save(tierList).getId();
	}

	public void updateDetails(long tierListId, TierListForm form) {
		getTierList(tierListId).updateDetails(form.getTitle(), form.getPatch(), now());
	}

	/**
	 * Publishes a draft and archives the previously published tier list.
	 * <p>
	 * The archive is flushed before publishing: the database allows only one PUBLISHED row (unique index),
	 * and Hibernate does not promise to send the two UPDATE statements in the order the methods were called.
	 * If publishing fails (e.g. the list is not a draft), the exception rolls back the whole transaction,
	 * including the archive.
	 */
	public void publish(long tierListId) {
		TierList tierList = getTierList(tierListId);
		Instant now = now();
		Optional<TierList> currentlyPublished = tierListRepository.findByStatus(TierListStatus.PUBLISHED);
		currentlyPublished.ifPresent(previous -> {
			previous.archive(now);
			tierListRepository.flush();
			log.info("Tier list {} ({}) archived", previous.getId(), previous.getTitle());
		});
		tierList.publish(now);
		log.info("Tier list {} ({}) published", tierList.getId(), tierList.getTitle());
	}

	public long copyToNewPatch(long tierListId, TierListForm form) {
		TierList copy = getTierList(tierListId).copyAsDraft(form.getTitle(), form.getPatch(), now());
		return tierListRepository.save(copy).getId();
	}

	public void delete(long tierListId) {
		TierList tierList = getTierList(tierListId);
		if (!tierList.isDeletable()) {
			throw new TierListRuleException("Only a draft can be deleted.");
		}
		tierListRepository.delete(tierList);
	}

	@Transactional(readOnly = true)
	public CompositionFormOptions getCompositionFormOptions(long tierListId) {
		TierList tierList = getTierList(tierListId);
		int setNumber = tierList.getSetNumber();
		List<ChampionOption> champions = championRepository.findAllBySetNumber(setNumber).stream()
				.sorted(Comparator.comparingInt(Champion::getCost).thenComparing(Champion::getName))
				.map(champion -> new ChampionOption(champion.getId(), champion.getName(), champion.getCost()))
				.toList();
		return new CompositionFormOptions(tierList.getId(), tierList.getTitle(), setNumber, champions,
				ItemChoices.forCompositions(itemRepository.findAllBySetNumber(setNumber), setNumber));
	}

	public void addComposition(long tierListId, CompositionForm form) {
		getTierList(tierListId).addComposition(toContent(form), now());
	}

	public void updateComposition(long tierListId, long compositionId, CompositionForm form) {
		getTierList(tierListId).updateComposition(compositionId, toContent(form), now());
	}

	public void deleteComposition(long tierListId, long compositionId) {
		getTierList(tierListId).removeComposition(compositionId, now());
	}

	private CompositionContent toContent(CompositionForm form) {
		List<UnitForm> selectedUnits = form.selectedUnits();
		Map<Long, Champion> champions = loadByIds(
				selectedUnits.stream().map(UnitForm::getChampionId).toList(),
				championRepository::findAllById, Champion::getId, "champion");
		Map<Long, Item> items = loadByIds(
				selectedUnits.stream().flatMap(unit -> unit.selectedItemIds().stream()).toList(),
				itemRepository::findAllById, Item::getId, "item");

		List<UnitContent> units = selectedUnits.stream()
				.map(unit -> new UnitContent(
						champions.get(unit.getChampionId()),
						unit.isCarry(),
						unit.getStarLevel(),
						unit.selectedItemIds().stream().map(items::get).toList()))
				.toList();
		return new CompositionContent(form.getName(), form.getTier(), form.getPosition(), form.getPlayStyle(),
				form.getDifficulty(), form.getTips(), units);
	}

	/** Loads entities with one query and fails if any of the requested ids does not exist. */
	private static <T> Map<Long, T> loadByIds(List<Long> ids,
											  Function<List<Long>, List<T>> finder,
											  Function<T, Long> idOf,
											  String kind) {
		List<Long> distinctIds = ids.stream().distinct().toList();
		Map<Long, T> found = finder.apply(distinctIds).stream()
				.collect(Collectors.toMap(idOf, Function.identity()));
		if (found.size() != distinctIds.size()) {
			throw new TierListRuleException("Unknown " + kind + " selected. Reload the page and try again.");
		}
		return found;
	}

	private void requireGameData(int setNumber) {
		if (!championRepository.existsBySetNumber(setNumber)) {
			throw new TierListRuleException("There is no game data for set " + setNumber + ". Import the set first.");
		}
	}

	private TierList getTierList(long tierListId) {
		return tierListRepository.findById(tierListId)
				.orElseThrow(() -> new TierListNotFoundException("Tier list " + tierListId + " not found"));
	}

	private Instant now() {
		return clock.instant();
	}
}
