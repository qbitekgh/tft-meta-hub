package pl.kubit.tft_meta_hub.tierlist;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class TierListNotFoundException extends RuntimeException {

	public TierListNotFoundException(String message) {
		super(message);
	}
}
