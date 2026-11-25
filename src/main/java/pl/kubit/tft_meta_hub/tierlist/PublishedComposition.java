package pl.kubit.tft_meta_hub.tierlist;

import pl.kubit.tft_meta_hub.tierlist.TierListDetails.CompositionView;

/** A composition of the published tier list, with the list's title and patch for the page header. */
public record PublishedComposition(String tierListTitle, String patch, CompositionView composition) {
}
