package pl.kubit.tft_meta_hub;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class TftMetaHubApplicationTests {

	@Test
	void contextLoads() {
	}

}
