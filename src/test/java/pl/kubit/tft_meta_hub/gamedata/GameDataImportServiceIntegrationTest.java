package pl.kubit.tft_meta_hub.gamedata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import pl.kubit.tft_meta_hub.TestcontainersConfiguration;
import pl.kubit.tft_meta_hub.gamedata.GameDataImportResult.ImportCounts;
import pl.kubit.tft_meta_hub.gamedata.cdragon.CdragonFixtures;
import pl.kubit.tft_meta_hub.gamedata.cdragon.CommunityDragonClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * Imports the sample data into a real PostgreSQL (Testcontainers); the HTTP client is mocked.
 * Each test runs in a transaction that is rolled back afterwards.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class GameDataImportServiceIntegrationTest {

	@MockitoBean
	private CommunityDragonClient communityDragonClient;

	@Autowired
	private GameDataImportService importService;

	@Autowired
	private ChampionRepository championRepository;

	@Autowired
	private TraitRepository traitRepository;

	@Autowired
	private ItemRepository itemRepository;

	@BeforeEach
	void setUp() {
		given(communityDragonClient.fetchTftData()).willReturn(CdragonFixtures.sampleData());
	}

	@Test
	void shouldAddAllRecords_whenSetIsImportedForTheFirstTime() {
		GameDataImportResult result = importService.importSet(18);

		assertThat(result).isEqualTo(new GameDataImportResult(18,
				new ImportCounts(4, 0), new ImportCounts(6, 0), new ImportCounts(8, 0)));
		assertThat(championRepository.findAllBySetNumber(18)).hasSize(4);
		assertThat(traitRepository.findAllBySetNumber(18)).hasSize(6);
		assertThat(itemRepository.findAllBySetNumber(18)).hasSize(8);
	}

	@Test
	void shouldUpdateInsteadOfDuplicating_whenSetIsImportedTwice() {
		importService.importSet(18);

		GameDataImportResult secondResult = importService.importSet(18);

		assertThat(secondResult).isEqualTo(new GameDataImportResult(18,
				new ImportCounts(0, 4), new ImportCounts(0, 6), new ImportCounts(0, 8)));
		assertThat(championRepository.count()).isEqualTo(4);
		assertThat(traitRepository.count()).isEqualTo(6);
		assertThat(itemRepository.count()).isEqualTo(8);
	}

	@Test
	void shouldLinkChampionWithTraits_whenTraitNamesMatch() {
		importService.importSet(18);

		Champion xayah = findChampion("DA_18_Xayah");
		assertThat(xayah.getTraits())
				.extracting(Trait::getApiName)
				.containsExactlyInAnyOrder("DA_18_Elderwood", "DA_18_Fae", "DA_18_Rapidfire");
		assertThat(xayah.getCost()).isEqualTo(1);
	}

	@Test
	void shouldStoreBreakpointsInOrder_whenTraitIsImported() {
		importService.importSet(18);

		Trait elderwood = traitRepository.findAllBySetNumber(18).stream()
				.filter(trait -> trait.getApiName().equals("DA_18_Elderwood"))
				.findFirst()
				.orElseThrow();
		assertThat(elderwood.getBreakpoints()).isEqualTo(List.of(3, 5, 7, 9, 11));
	}

	@Test
	void shouldThrowAndSaveNothing_whenSetIsMissing() {
		assertThatThrownBy(() -> importService.importSet(19)).isInstanceOf(SetNotFoundException.class);

		assertThat(championRepository.count()).isZero();
	}

	private Champion findChampion(String apiName) {
		return championRepository.findAllBySetNumber(18).stream()
				.filter(champion -> champion.getApiName().equals(apiName))
				.findFirst()
				.orElseThrow();
	}
}
