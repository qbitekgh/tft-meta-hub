package pl.kubit.tft_meta_hub.gamedata;

/** A patch cannot be added; the message is meant to be shown to the admin. */
public class GamePatchException extends RuntimeException {

	public GamePatchException(String message) {
		super(message);
	}
}
