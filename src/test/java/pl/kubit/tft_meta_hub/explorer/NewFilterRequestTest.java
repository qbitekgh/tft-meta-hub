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

class NewFilterRequestTest {

	@Test
	void shouldBuildFilterOfFormType_whenFormIsComplete() {
		assertThat(new NewFilterRequest("unit", AHRI, 2, 3, 1, 3, null, null, null).toFilter())
				.contains(new UnitFilter(AHRI, 2, 3, 1, 3));
		assertThat(new NewFilterRequest("unititem", AHRI, null, null, null, null, GUINSOO, null, null).toFilter())
				.contains(new UnitItemFilter(AHRI, GUINSOO));
		assertThat(new NewFilterRequest("item", null, null, null, null, null, GUINSOO, null, null).toFilter())
				.contains(new ItemFilter(GUINSOO));
		assertThat(new NewFilterRequest("trait", null, null, null, null, null, null, BRAWLER, 4).toFilter())
				.contains(new TraitFilter(BRAWLER, 4));
	}

	@Test
	void shouldUseAnyRange_whenRangesAreMissing() {
		assertThat(new NewFilterRequest("unit", AHRI, null, null, null, null, null, null, null).toFilter())
				.contains(UnitFilter.of(AHRI));
	}

	@Test
	void shouldReturnEmpty_whenFormIsIncompleteOrInvalid() {
		assertThat(new NewFilterRequest(null, AHRI, null, null, null, null, null, null, null).toFilter()).isEmpty();
		assertThat(new NewFilterRequest("unit", "", null, null, null, null, null, null, null).toFilter()).isEmpty();
		assertThat(new NewFilterRequest("unit", AHRI, 3, 1, null, null, null, null, null).toFilter()).isEmpty();
		assertThat(new NewFilterRequest("unititem", AHRI, null, null, null, null, null, null, null).toFilter()).isEmpty();
		assertThat(new NewFilterRequest("comp", AHRI, null, null, null, null, null, null, null).toFilter()).isEmpty();
	}
}
