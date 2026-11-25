package pl.kubit.tft_meta_hub.collector;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.kubit.tft_meta_hub.riot.Platform;

import java.time.Instant;

/**
 * Collector state for one ladder player: when we last downloaded their match list. The PUUID is needed to ask Riot
 * for match ids; it is stored only here and never shown anywhere.
 */
@Entity
@Table(name = "tracked_player")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrackedPlayer {

	@Id
	private String puuid;

	@Enumerated(EnumType.STRING)
	private Platform platform;

	private Instant lastSeenOnLadderAt;

	/** Null until the first successful download of the player's match list. */
	private Instant lastFetchedAt;

	static TrackedPlayer create(String puuid, Platform platform, Instant seenAt) {
		TrackedPlayer player = new TrackedPlayer();
		player.puuid = puuid;
		player.platform = platform;
		player.lastSeenOnLadderAt = seenAt;
		return player;
	}

	void markSeenOnLadder(Platform platform, Instant seenAt) {
		this.platform = platform;
		this.lastSeenOnLadderAt = seenAt;
	}

	void markFetched(Instant fetchedAt) {
		this.lastFetchedAt = fetchedAt;
	}
}
