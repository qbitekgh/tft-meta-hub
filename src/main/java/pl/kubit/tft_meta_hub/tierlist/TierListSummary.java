package pl.kubit.tft_meta_hub.tierlist;

import java.time.Instant;

/** One row of the admin's list of tier lists. */
public record TierListSummary(long id,
							  String title,
							  String patch,
							  int setNumber,
							  TierListStatus status,
							  Instant publishedAt,
							  Instant updatedAt,
							  int compositionCount) {
}
