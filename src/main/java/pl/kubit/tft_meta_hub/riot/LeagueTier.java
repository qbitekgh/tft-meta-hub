package pl.kubit.tft_meta_hub.riot;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Ladders the collector reads; each has its own endpoint. */
@Getter
@RequiredArgsConstructor
public enum LeagueTier {
	CHALLENGER("challenger"),
	GRANDMASTER("grandmaster");

	private final String path;
}
