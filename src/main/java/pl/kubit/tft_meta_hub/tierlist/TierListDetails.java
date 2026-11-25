package pl.kubit.tft_meta_hub.tierlist;

import java.time.Instant;
import java.util.List;

/**
 * A tier list with all compositions (in display order), as read-only data for views.
 *
 * @param editable  false for archived tier lists
 * @param deletable true only for drafts
 */
public record TierListDetails(long id,
							  String title,
							  String patch,
							  int setNumber,
							  TierListStatus status,
							  Instant publishedAt,
							  Instant updatedAt,
							  boolean editable,
							  boolean deletable,
							  List<CompositionView> compositions) {

	public record CompositionView(long id,
								  String name,
								  Tier tier,
								  int position,
								  PlayStyle playStyle,
								  Difficulty difficulty,
								  String tips,
								  List<UnitView> units) {
	}

	public record UnitView(long championId,
						   String championName,
						   String championIconUrl,
						   int cost,
						   boolean carry,
						   int starLevel,
						   List<ItemView> items) {
	}

	public record ItemView(long id, String name, String iconUrl) {
	}
}
