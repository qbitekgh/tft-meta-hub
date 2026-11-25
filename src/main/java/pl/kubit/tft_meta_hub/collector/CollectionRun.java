package pl.kubit.tft_meta_hub.collector;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** One execution of the collector, shown as "last run" in the admin panel. */
@Entity
@Table(name = "collection_run")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollectionRun {

	private static final int MAX_MESSAGE_LENGTH = 500;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "run_trigger")
	private RunTrigger trigger;

	@Enumerated(EnumType.STRING)
	private RunStatus status;

	private Instant startedAt;

	private Instant finishedAt;

	private int playersChecked;

	private int matchesSaved;

	private int matchesSkipped;

	private String errorMessage;

	static CollectionRun start(RunTrigger trigger, Instant now) {
		CollectionRun run = new CollectionRun();
		run.trigger = trigger;
		run.status = RunStatus.RUNNING;
		run.startedAt = now;
		return run;
	}

	void finish(RunStatus status, RunProgress progress, String errorMessage, Instant now) {
		if (this.status != RunStatus.RUNNING) {
			throw new IllegalStateException("Collection run " + id + " is already finished");
		}
		this.status = status;
		this.playersChecked = progress.playersChecked();
		this.matchesSaved = progress.matchesSaved();
		this.matchesSkipped = progress.matchesSkipped();
		this.errorMessage = errorMessage == null || errorMessage.length() <= MAX_MESSAGE_LENGTH
				? errorMessage : errorMessage.substring(0, MAX_MESSAGE_LENGTH);
		this.finishedAt = now;
	}

	/** A run still marked RUNNING after a restart was cut off when the application stopped. */
	void markInterrupted(Instant now) {
		finish(RunStatus.FAILED, new RunProgress(), "Interrupted by an application restart", now);
	}
}
