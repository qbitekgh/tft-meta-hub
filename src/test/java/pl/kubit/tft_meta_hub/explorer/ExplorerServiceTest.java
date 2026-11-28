package pl.kubit.tft_meta_hub.explorer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.kubit.tft_meta_hub.explorer.ExplorerFilter.TraitFilter;
import pl.kubit.tft_meta_hub.gamedata.GameDataCatalogService;
import pl.kubit.tft_meta_hub.gamedata.GamePatchService;
import pl.kubit.tft_meta_hub.gamedata.GamePatchView;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static pl.kubit.tft_meta_hub.explorer.ExplorerTestData.AHRI;
import static pl.kubit.tft_meta_hub.explorer.ExplorerTestData.BRAWLER;
import static pl.kubit.tft_meta_hub.explorer.ExplorerTestData.catalog;
import static pl.kubit.tft_meta_hub.explorer.ExplorerTestData.row;

@ExtendWith(MockitoExtension.class)
class ExplorerServiceTest {

	private static final Instant NOW = Instant.parse("2026-11-28T12:00:00Z");

	@Mock
	private ExplorerQueryRepository queryRepository;

	@Mock
	private GamePatchService gamePatchService;

	@Mock
	private GameDataCatalogService gameDataCatalogService;

	private ExplorerService service;

	@BeforeEach
	void setUp() {
		service = new ExplorerService(queryRepository, gamePatchService, gameDataCatalogService,
				Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void shouldReturnPageWithoutData_whenNoPatchIsKnown() {
		given(gamePatchService.findAll()).willReturn(List.of());

		ExplorerPage page = service.explore(ExplorerRequest.empty());

		assertThat(page.hasPatch()).isFalse();
		assertThat(page.summary()).isNull();
		verifyNoInteractions(queryRepository, gameDataCatalogService);
	}

	@Test
	void shouldQueryNewestPatchOfLastDays_whenRequestIsEmpty() {
		givenPatches();
		given(queryRepository.placements(any())).willReturn(distribution(4, 4, 4, 4, 4, 4, 4, 4));
		given(queryRepository.unitStats(any(), anyBoolean())).willReturn(List.of(row(AHRI, 0, 12, 3.9)));

		ExplorerPage page = service.explore(ExplorerRequest.empty());

		ArgumentCaptor<BoardSelection> selection = ArgumentCaptor.forClass(BoardSelection.class);
		verify(queryRepository).placements(selection.capture());
		assertThat(selection.getValue()).isEqualTo(new BoardSelection("18.7", null,
				Instant.parse("2026-11-14T12:00:00Z"), List.of()));
		assertThat(page.patches()).containsExactly("18.7", "18.6");
		assertThat(page.summary().stats().games()).isEqualTo(32);
		assertThat(page.summary().share()).isCloseTo(1.0, within(1e-9));
		assertThat(page.rows()).extracting(ExplorerPage.ExplorerRow::name).containsExactly("Ahri");
		assertThat(page.options().champions()).isNotEmpty();
	}

	@Test
	void shouldCompareWithAllBoards_whenFiltersAreSet() {
		givenPatches();
		BoardSelection filtered = new BoardSelection("18.7", null, Instant.parse("2026-11-14T12:00:00Z"),
				List.of(new TraitFilter(BRAWLER, 4)));
		given(queryRepository.placements(filtered)).willReturn(distribution(2, 1, 1, 0, 0, 0, 0, 0));
		given(queryRepository.placements(filtered.withoutFilters())).willReturn(distribution(4, 4, 4, 4, 4, 4, 4, 4));

		ExplorerPage page = service.explore(new ExplorerRequest(null, null, null, List.of("trait:DA_18_Brawler:4"),
				"TRAITS", null, null, null, null, null));

		assertThat(page.summary().stats().games()).isEqualTo(4);
		assertThat(page.summary().allGames()).isEqualTo(32);
		assertThat(page.summary().share()).isCloseTo(0.125, within(1e-9));
		assertThat(page.filters()).extracting(ExplorerPage.ActiveFilter::label).containsExactly("Brawler 4+");
		verify(queryRepository).traitStats(filtered);
	}

	@Test
	void shouldNotQueryBuilds_whenNoItemHolderIsChosen() {
		givenPatches();
		given(queryRepository.placements(any())).willReturn(distribution(1, 1, 1, 1, 1, 1, 1, 1));

		ExplorerPage page = service.explore(new ExplorerRequest(null, null, null, null, "BUILDS", null, null, null,
				null, null));

		assertThat(page.rows()).isEmpty();
		verify(queryRepository, never()).buildStats(any(), any());
		// without filters the summary already covers all boards, so a second query is not needed
		verify(queryRepository, times(1)).placements(any());
	}

	private void givenPatches() {
		given(gamePatchService.findAll()).willReturn(List.of(
				new GamePatchView(2, "18.7", 18, Instant.parse("2026-11-17T16:20:00Z")),
				new GamePatchView(1, "18.6", 18, Instant.parse("2026-11-03T16:20:00Z"))));
		given(gameDataCatalogService.getCatalog(18)).willReturn(catalog());
	}

	private static PlacementDistribution distribution(long... counts) {
		return new PlacementDistribution(Arrays.stream(counts).boxed().toList());
	}
}
