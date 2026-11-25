package pl.kubit.tft_meta_hub.gamedata;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** A game patch and the moment it went live; entered by the admin because Riot's match data has no patch number. */
@Entity
@Table(name = "game_patch")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GamePatch {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** E.g. "18.1". */
	private String patch;

	private int setNumber;

	private Instant startsAt;

	public static GamePatch create(String patch, int setNumber, Instant startsAt) {
		GamePatch gamePatch = new GamePatch();
		gamePatch.patch = patch.strip();
		gamePatch.setNumber = setNumber;
		gamePatch.startsAt = startsAt;
		return gamePatch;
	}
}
