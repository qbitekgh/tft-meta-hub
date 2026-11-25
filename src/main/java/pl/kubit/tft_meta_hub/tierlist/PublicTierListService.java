package pl.kubit.tft_meta_hub.tierlist;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.kubit.tft_meta_hub.tierlist.TierListDetails.CompositionView;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Read-only use cases of the public site. Visitors only ever see the currently published tier list;
 * drafts and archived lists (and their compositions) stay hidden.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicTierListService {

	private final TierListRepository tierListRepository;

	public Optional<PublishedTierList> findPublished() {
		return findPublishedDetails().map(details -> new PublishedTierList(
				details.title(), details.patch(), details.setNumber(), details.publishedAt(),
				groupByTier(details.compositions())));
	}

	/** @throws TierListNotFoundException when the composition is not part of the published tier list */
	public PublishedComposition getComposition(long compositionId) {
		TierListDetails details = findPublishedDetails()
				.orElseThrow(() -> new TierListNotFoundException("No tier list is published"));
		return details.compositions().stream()
				.filter(composition -> composition.id() == compositionId)
				.findFirst()
				.map(composition -> new PublishedComposition(details.title(), details.patch(), composition))
				.orElseThrow(() -> new TierListNotFoundException("Composition " + compositionId + " is not published"));
	}

	/**
	 * Groups compositions by tier. The EnumMap keeps tiers in declaration order (S, A, B, C, Situational)
	 * and the input order (by position) is kept inside each tier. Tiers without compositions are absent.
	 */
	static Map<Tier, List<CompositionView>> groupByTier(List<CompositionView> compositions) {
		return compositions.stream()
				.collect(Collectors.groupingBy(
						CompositionView::tier,
						() -> new EnumMap<>(Tier.class),
						Collectors.toList()));
	}

	private Optional<TierListDetails> findPublishedDetails() {
		return tierListRepository.findByStatus(TierListStatus.PUBLISHED)
				.map(TierListViews::toDetails);
	}
}
