package pl.kubit.tft_meta_hub;

import org.springframework.boot.SpringApplication;

public class TestTftMetaHubApplication {

	public static void main(String[] args) {
		SpringApplication.from(TftMetaHubApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
