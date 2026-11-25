package pl.kubit.tft_meta_hub.gamedata.cdragon;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CommunityDragonClientTest {

	@Test
	void shouldDownloadAndParseTftData_whenServerReturnsDataFile() {
		RestClient.Builder builder = RestClient.builder().baseUrl(CdragonFixtures.PROPERTIES.baseUrl());
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		CommunityDragonClient client = new CommunityDragonClient(builder.build(), CdragonFixtures.PROPERTIES);
		server.expect(requestTo("https://cdragon.test/latest/cdragon/tft/en_us.json"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(CdragonFixtures.sampleJson(), MediaType.APPLICATION_JSON));

		CdragonData data = client.fetchTftData();

		server.verify();
		assertThat(data.sets()).containsOnlyKeys("18");
		assertThat(data.sets().get("18").champions()).hasSize(5);
		assertThat(data.items()).hasSize(13);
		assertThat(data.setData()).extracting(CdragonData.CdragonSetData::mutator)
				.containsExactly("TFTSet18", "TFTSet18_PAIRS");
	}
}
