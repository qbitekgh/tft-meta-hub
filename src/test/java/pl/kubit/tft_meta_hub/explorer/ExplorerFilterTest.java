package pl.kubit.tft_meta_hub.explorer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import pl.kubit.tft_meta_hub.explorer.ExplorerFilter.ItemFilter;
import pl.kubit.tft_meta_hub.explorer.ExplorerFilter.TraitFilter;
import pl.kubit.tft_meta_hub.explorer.ExplorerFilter.UnitFilter;
import pl.kubit.tft_meta_hub.explorer.ExplorerFilter.UnitItemFilter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExplorerFilterTest {

	@Test
	void shouldParseEveryFilterType_whenParamsAreValid() {
		assertThat(ExplorerFilter.parse("unit:TFT18_Ahri:2-3:1-3")).contains(new UnitFilter("TFT18_Ahri", 2, 3, 1, 3));
		assertThat(ExplorerFilter.parse("unititem:TFT18_Ahri:DA_GuinsoosRageblade"))
				.contains(new UnitItemFilter("TFT18_Ahri", "DA_GuinsoosRageblade"));
		assertThat(ExplorerFilter.parse("item:DA_18_EmblemFae")).contains(new ItemFilter("DA_18_EmblemFae"));
		assertThat(ExplorerFilter.parse("trait:DA_18_Brawler:4")).contains(new TraitFilter("DA_18_Brawler", 4));
	}

	@Test
	void shouldReturnSameFilter_whenParamIsParsedBack() {
		List<ExplorerFilter> filters = List.of(
				new UnitFilter("TFT18_Ahri", 1, 2, 0, 3),
				new UnitItemFilter("TFT18_Ahri", "DA_InfinityEdge"),
				new ItemFilter("DA_18_EmblemFae"),
				new TraitFilter("DA_18_Brawler", 2));

		assertThat(filters).allSatisfy(filter -> assertThat(ExplorerFilter.parse(filter.toParam())).contains(filter));
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {
			"",
			"champion:TFT18_Ahri",              // unknown type
			"unit:TFT18_Ahri",                  // missing ranges
			"unit:TFT18_Ahri:3-1:0-3",          // min stars above max stars
			"unit:TFT18_Ahri:0-3:0-3",          // no 0-star units
			"unit:TFT18_Ahri:1-3:0-4",          // at most 3 items
			"unit:TFT18_Ahri:a-b:0-3",          // not numbers
			"item:DA_Item' OR '1'='1",          // not an api name
			"item:",                            // empty api name
			"trait:DA_18_Brawler:0",            // at least 1 unit
			"trait:DA_18_Brawler:4:extra"})     // too many parts
	void shouldReturnEmpty_whenParamIsInvalid(String param) {
		assertThat(ExplorerFilter.parse(param)).isEmpty();
	}

	@Test
	void shouldThrow_whenFilterIsCreatedWithInvalidApiName() {
		assertThatThrownBy(() -> new ItemFilter("DA_Item:Other"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldCoverAnyStarsAndItems_whenUnitFilterIsCreatedForUnitOnly() {
		assertThat(UnitFilter.of("TFT18_Ahri").toParam()).isEqualTo("unit:TFT18_Ahri:1-3:0-3");
	}
}
