package pl.kubit.tft_meta_hub.gamedata;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.kubit.tft_meta_hub.gamedata.cdragon.CdragonMapper;
import pl.kubit.tft_meta_hub.gamedata.cdragon.CommunityDragonClient;

/**
 * Imports champions, traits and items of one set from Community Dragon.
 * <p>
 * The download (about 24 MB) happens outside of a database transaction; only saving is transactional
 * and lives in a separate bean ({@link GameDataWriter}), because {@code @Transactional} does not work
 * when a method calls another method of the same object.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameDataImportService {

	private final CommunityDragonClient communityDragonClient;
	private final CdragonMapper cdragonMapper;
	private final GameDataWriter gameDataWriter;

	public GameDataImportResult importSet(int setNumber) {
		GameData gameData = cdragonMapper.map(communityDragonClient.fetchTftData(), setNumber);
		GameDataImportResult result = gameDataWriter.save(gameData);
		log.info("Game data import of set {} finished: champions {}, traits {}, items {}",
				setNumber, result.champions(), result.traits(), result.items());
		return result;
	}
}
