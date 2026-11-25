package pl.kubit.tft_meta_hub.collector;

public enum RunStatus {
	RUNNING,
	SUCCEEDED,
	/** Stopped because Riot rejected the API key (HTTP 401/403); a development key expires every 24 hours. */
	KEY_INVALID,
	FAILED
}
