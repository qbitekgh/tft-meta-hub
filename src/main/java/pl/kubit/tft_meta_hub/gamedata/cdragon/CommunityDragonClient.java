package pl.kubit.tft_meta_hub.gamedata.cdragon;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class CommunityDragonClient {

	private final RestClient restClient;
	private final CommunityDragonProperties properties;

	public CommunityDragonClient(@Qualifier("communityDragonRestClient") RestClient restClient,
								 CommunityDragonProperties properties) {
		this.restClient = restClient;
		this.properties = properties;
	}

	public CdragonData fetchTftData() {
		log.debug("Downloading TFT data from Community Dragon: {}{}", properties.baseUrl(), properties.dataPath());
		return restClient.get()
				.uri(properties.dataPath())
				.retrieve()
				.body(CdragonData.class);
	}
}
