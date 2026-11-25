package pl.kubit.tft_meta_hub.collector;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.kubit.tft_meta_hub.riot.Platform;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A collected ranked match with the boards of its players. The id is Riot's match id, e.g. "EUN1_4027724406". */
@Entity
@Table(name = "tft_match")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TftMatch {

	@Id
	private String id;

	@Enumerated(EnumType.STRING)
	private Platform platform;

	/** When the game ended. */
	private Instant gameDatetime;

	/** Raw version text from Riot, kept for reference; it no longer contains the patch number. */
	private String gameVersion;

	/** Assigned from the admin's patch list; null when the game is older than every known patch. */
	private String patch;

	private int setNumber;

	private Instant collectedAt;

	@OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("placement")
	private List<Participant> participants = new ArrayList<>();

	static TftMatch create(String id, Platform platform, Instant gameDatetime, String gameVersion, String patch,
						   int setNumber, Instant collectedAt) {
		TftMatch match = new TftMatch();
		match.id = id;
		match.platform = platform;
		match.gameDatetime = gameDatetime;
		match.gameVersion = gameVersion;
		match.patch = patch;
		match.setNumber = setNumber;
		match.collectedAt = collectedAt;
		return match;
	}

	Participant addParticipant(int placement, int level) {
		Participant participant = Participant.create(this, placement, level);
		participants.add(participant);
		return participant;
	}

	public List<Participant> getParticipants() {
		return Collections.unmodifiableList(participants);
	}
}
