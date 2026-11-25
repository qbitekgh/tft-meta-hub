package pl.kubit.tft_meta_hub.collector;

public enum RunTrigger {
	/** "Collect now" in the admin panel. */
	MANUAL,
	/** The optional {@code @Scheduled} job. */
	SCHEDULED
}
