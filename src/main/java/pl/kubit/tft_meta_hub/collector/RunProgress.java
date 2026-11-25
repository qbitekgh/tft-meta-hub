package pl.kubit.tft_meta_hub.collector;

/**
 * Counters of a running collection. Used by a single thread (the collector's), so plain fields are enough.
 */
class RunProgress {

	private int playersChecked;
	private int matchesSaved;
	private int matchesSkipped;
	private int failedRequests;

	void playerChecked() {
		playersChecked++;
	}

	void matchSaved() {
		matchesSaved++;
	}

	void matchSkipped() {
		matchesSkipped++;
	}

	void requestFailed() {
		failedRequests++;
	}

	int playersChecked() {
		return playersChecked;
	}

	int matchesSaved() {
		return matchesSaved;
	}

	int matchesSkipped() {
		return matchesSkipped;
	}

	int failedRequests() {
		return failedRequests;
	}
}
