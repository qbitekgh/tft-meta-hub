package pl.kubit.tft_meta_hub.tierlist;

import pl.kubit.tft_meta_hub.tierlist.TierListDetails.CompositionView;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * The tier list visitors see.
 *
 * @param compositionsByTier only tiers that have compositions, in tier order (an EnumMap);
 *                           compositions inside a tier are ordered by position
 */
public record PublishedTierList(String title,
								String patch,
								int setNumber,
								Instant publishedAt,
								Map<Tier, List<CompositionView>> compositionsByTier) {
}
