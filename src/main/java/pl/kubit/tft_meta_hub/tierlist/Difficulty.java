package pl.kubit.tft_meta_hub.tierlist;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Difficulty {
	EASY("Easy"),
	MEDIUM("Medium"),
	HARD("Hard");

	private final String label;
}
