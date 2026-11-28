package pl.kubit.tft_meta_hub.explorer;

import org.junit.jupiter.api.Test;
import pl.kubit.tft_meta_hub.explorer.ExplorerFilter.ItemFilter;
import pl.kubit.tft_meta_hub.explorer.ExplorerFilter.TraitFilter;
import pl.kubit.tft_meta_hub.explorer.ExplorerFilter.UnitFilter;
import pl.kubit.tft_meta_hub.explorer.ExplorerFilter.UnitItemFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.kubit.tft_meta_hub.explorer.ExplorerTestData.AHRI;
import static pl.kubit.tft_meta_hub.explorer.ExplorerTestData.BRAWLER;
import static pl.kubit.tft_meta_hub.explorer.ExplorerTestData.GUINSOO;
import static pl.kubit.tft_meta_hub.explorer.ExplorerTestData.POTION;
import static pl.kubit.tft_meta_hub.explorer.ExplorerTestData.catalog;

class FilterLabelsTest {

	@Test
	void shouldShowOnlyNarrowingParts_whenUnitFilterIsLabelled() {
		assertThat(FilterLabels.label(UnitFilter.of(AHRI), catalog())).isEqualTo("Ahri");
		assertThat(FilterLabels.label(new UnitFilter(AHRI, 2, 3, 0, 3), catalog())).isEqualTo("Ahri 2–3★");
		assertThat(FilterLabels.label(new UnitFilter(AHRI, 3, 3, 2, 3), catalog())).isEqualTo("Ahri 3★, 2–3 items");
		assertThat(FilterLabels.label(new UnitFilter(AHRI, 1, 3, 1, 1), catalog())).isEqualTo("Ahri 1 item");
	}

	@Test
	void shouldUseGameDataNames_whenOtherFiltersAreLabelled() {
		assertThat(FilterLabels.label(new UnitItemFilter(AHRI, GUINSOO), catalog())).isEqualTo("Guinsoo's Rageblade on Ahri");
		assertThat(FilterLabels.label(new ItemFilter(GUINSOO), catalog())).isEqualTo("Guinsoo's Rageblade");
		assertThat(FilterLabels.label(new TraitFilter(BRAWLER, 4), catalog())).isEqualTo("Brawler 4+");
	}

	@Test
	void shouldFallBackToApiName_whenElementIsMissingFromGameData() {
		assertThat(FilterLabels.label(new ItemFilter(POTION), catalog())).isEqualTo(POTION);
	}
}
