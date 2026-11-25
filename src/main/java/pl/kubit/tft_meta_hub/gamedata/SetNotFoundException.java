package pl.kubit.tft_meta_hub.gamedata;

public class SetNotFoundException extends RuntimeException {

	public SetNotFoundException(int setNumber) {
		super("Set " + setNumber + " was not found in Community Dragon data");
	}
}
