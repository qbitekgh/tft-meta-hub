package pl.kubit.tft_meta_hub.tierlist;

/** A business rule of tier lists was broken; the message is meant to be shown to the admin. */
public class TierListRuleException extends RuntimeException {

	public TierListRuleException(String message) {
		super(message);
	}
}
