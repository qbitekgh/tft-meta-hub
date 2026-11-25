package pl.kubit.tft_meta_hub.collector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.kubit.tft_meta_hub.gamedata.GamePatchService;
import pl.kubit.tft_meta_hub.gamedata.GamePatchView;
import pl.kubit.tft_meta_hub.gamedata.PatchCalendar;
import pl.kubit.tft_meta_hub.riot.LeagueTier;
import pl.kubit.tft_meta_hub.riot.Platform;
import pl.kubit.tft_meta_hub.riot.RiotApiClient;
import pl.kubit.tft_meta_hub.riot.RiotApiResult;
import pl.kubit.tft_meta_hub.riot.RiotApiResult.Failure;
import pl.kubit.tft_meta_hub.riot.RiotApiResult.KeyInvalid;
import pl.kubit.tft_meta_hub.riot.RiotApiResult.NotFound;
import pl.kubit.tft_meta_hub.riot.RiotApiResult.RateLimited;
import pl.kubit.tft_meta_hub.riot.RiotApiResult.Success;
import pl.kubit.tft_meta_hub.riot.dto.LeagueListDto;
import pl.kubit.tft_meta_hub.riot.dto.MatchDto;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * One collection run: ladder players → their new match ids → match details → save ranked matches of the current set.
 * <p>
 * The run is long (minutes, limited by Riot's rate limits), so there is no transaction around it: every match and
 * every player update is saved on its own, and an interrupted run keeps what it has already collected.
 * Riot answers are handled with a {@code switch} over the sealed {@link RiotApiResult}; an invalid key or exhausted
 * rate limit stops the whole run through {@link StopRun}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchCollectorService {

	static final String NO_PATCH_MESSAGE = "No patch configured. Add the current patch in the admin panel first.";
	static final String RATE_LIMITED_MESSAGE = "Riot API rate limit exceeded.";

	private final RiotApiClient riotApiClient;
	private final GamePatchService gamePatchService;
	private final TftMatchRepository matchRepository;
	private final TrackedPlayerRepository trackedPlayerRepository;
	private final CollectionRunRepository collectionRunRepository;
	private final CollectorProperties properties;
	private final Clock clock;

	public CollectionRun collect(RunTrigger trigger) {
		Instant startedAt = clock.instant();
		CollectionRun run = collectionRunRepository.save(CollectionRun.start(trigger, startedAt));
		RunProgress progress = new RunProgress();
		log.info("Match collection started ({})", trigger);
		try {
			PatchCalendar patches = gamePatchService.getCalendar();
			int currentSet = patches.latest()
					.map(GamePatchView::setNumber)
					.orElseThrow(() -> new StopRun(RunStatus.FAILED, NO_PATCH_MESSAGE));

			List<TrackedPlayer> players = refreshLadders(startedAt);
			Set<String> seenMatchIds = new HashSet<>();
			for (TrackedPlayer player : players) {
				if (limitReached(progress)) {
					break;
				}
				collectPlayer(player, currentSet, patches, startedAt, seenMatchIds, progress);
			}
			int deleted = deleteOldMatches();
			run.finish(RunStatus.SUCCEEDED, progress, null, clock.instant());
			log.info("Match collection finished: {} players checked, {} matches saved, {} skipped, {} failed requests, "
							+ "{} old matches deleted",
					progress.playersChecked(), progress.matchesSaved(), progress.matchesSkipped(),
					progress.failedRequests(), deleted);
		} catch (StopRun stop) {
			run.finish(stop.status, progress, stop.getMessage(), clock.instant());
			log.warn("Match collection stopped ({}): {} Saved {} matches before stopping.",
					stop.status, stop.getMessage(), progress.matchesSaved());
		} catch (RuntimeException e) {
			run.finish(RunStatus.FAILED, progress, e.getMessage(), clock.instant());
			log.error("Match collection failed", e);
		}
		return collectionRunRepository.save(run);
	}

	/**
	 * Reads every configured ladder, saves new players and marks all of them as seen.
	 *
	 * @return players on the ladders right now; never-fetched players first, then the longest-waiting ones
	 */
	private List<TrackedPlayer> refreshLadders(Instant now) {
		Map<String, Platform> onLadder = new LinkedHashMap<>();
		for (Platform platform : Platform.values()) {
			for (LeagueTier tier : LeagueTier.values()) {
				switch (riotApiClient.getLeague(platform, tier)) {
					case Success<LeagueListDto>(LeagueListDto ladder) ->
							ladder.entries().forEach(entry -> onLadder.put(entry.puuid(), platform));
					case KeyInvalid<LeagueListDto> keyInvalid -> throw StopRun.keyInvalid();
					case RateLimited<LeagueListDto> rateLimited -> throw StopRun.rateLimited();
					case NotFound<LeagueListDto> notFound -> log.warn("Ladder {} {} not found", platform, tier);
					case Failure<LeagueListDto>(String message) ->
							log.warn("Ladder {} {} could not be read: {}", platform, tier, message);
				}
			}
		}

		Map<String, TrackedPlayer> known = trackedPlayerRepository.findAllById(onLadder.keySet()).stream()
				.collect(Collectors.toMap(TrackedPlayer::getPuuid, Function.identity()));
		List<TrackedPlayer> players = new ArrayList<>();
		onLadder.forEach((puuid, platform) -> {
			TrackedPlayer player = known.get(puuid);
			if (player == null) {
				player = TrackedPlayer.create(puuid, platform, now);
			} else {
				player.markSeenOnLadder(platform, now);
			}
			players.add(player);
		});
		log.info("Ladders read: {} players ({} new)", players.size(), players.size() - known.size());
		return trackedPlayerRepository.saveAll(players).stream()
				.sorted(Comparator.comparing(TrackedPlayer::getLastFetchedAt, Comparator.nullsFirst(Comparator.naturalOrder())))
				.toList();
	}

	/**
	 * Downloads the player's new matches. The player is marked as fetched (at the run's start time, so games that end
	 * during the run are not missed next time) only after all their match ids were processed.
	 */
	private void collectPlayer(TrackedPlayer player, int currentSet, PatchCalendar patches, Instant runStartedAt,
							   Set<String> seenMatchIds, RunProgress progress) {
		Instant since = player.getLastFetchedAt() != null
				? player.getLastFetchedAt()
				: runStartedAt.minus(Duration.ofDays(properties.retentionDays()));

		List<String> matchIds;
		switch (riotApiClient.getMatchIds(player.getPuuid(), since, properties.matchIdsPerPlayer())) {
			case Success<List<String>>(List<String> ids) -> matchIds = ids;
			case NotFound<List<String>> notFound -> matchIds = List.of();
			case KeyInvalid<List<String>> keyInvalid -> throw StopRun.keyInvalid();
			case RateLimited<List<String>> rateLimited -> throw StopRun.rateLimited();
			case Failure<List<String>> failure -> {
				// try this player again in the next run
				progress.requestFailed();
				return;
			}
		}

		for (String matchId : matchIds) {
			if (limitReached(progress)) {
				return;
			}
			if (!seenMatchIds.add(matchId) || matchRepository.existsById(matchId)) {
				progress.matchSkipped();
				continue;
			}
			collectMatch(matchId, currentSet, patches, progress);
		}
		player.markFetched(runStartedAt);
		trackedPlayerRepository.save(player);
		progress.playerChecked();
	}

	private void collectMatch(String matchId, int currentSet, PatchCalendar patches, RunProgress progress) {
		switch (riotApiClient.getMatch(matchId)) {
			case Success<MatchDto>(MatchDto match) when MatchMapper.isCollectable(match, currentSet) -> {
				matchRepository.save(MatchMapper.toEntity(match, patches, clock.instant()));
				progress.matchSaved();
			}
			case Success<MatchDto> otherQueueOrSet -> progress.matchSkipped();
			case NotFound<MatchDto> notFound -> progress.matchSkipped();
			case KeyInvalid<MatchDto> keyInvalid -> throw StopRun.keyInvalid();
			case RateLimited<MatchDto> rateLimited -> throw StopRun.rateLimited();
			case Failure<MatchDto> failure -> progress.requestFailed();
		}
	}

	private boolean limitReached(RunProgress progress) {
		return progress.matchesSaved() >= properties.maxMatchesPerRun();
	}

	private int deleteOldMatches() {
		Instant cutoff = clock.instant().minus(Duration.ofDays(properties.retentionDays()));
		return matchRepository.deleteAllPlayedBefore(cutoff);
	}

	/** Ends the whole run from deep inside the loops with a status and a message for the admin panel. */
	static final class StopRun extends RuntimeException {

		private final RunStatus status;

		StopRun(RunStatus status, String message) {
			super(message);
			this.status = status;
		}

		static StopRun keyInvalid() {
			return new StopRun(RunStatus.KEY_INVALID, "Riot API key invalid or expired.");
		}

		static StopRun rateLimited() {
			return new StopRun(RunStatus.FAILED, RATE_LIMITED_MESSAGE);
		}
	}
}
