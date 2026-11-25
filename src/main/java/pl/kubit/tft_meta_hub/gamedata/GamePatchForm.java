package pl.kubit.tft_meta_hub.gamedata;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * Input of the "add patch" form. A mutable bean rather than a record: Spring binds each field separately, so
 * an unreadable date does not stop the validation of the other fields.
 */
@Getter
@Setter
@NoArgsConstructor
public class GamePatchForm {

	@NotBlank
	@Pattern(regexp = "\\d{1,2}\\.\\d{1,2}[a-z]?", message = "must look like 16.3 or 16.3b")
	private String patch;

	@NotNull
	@Min(1)
	@Max(99)
	private Integer setNumber;

	/** When the patch went live, in UTC (an HTML {@code datetime-local} field, e.g. "2026-11-19T10:00"). */
	@NotNull
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	private LocalDateTime startsAt;

	public GamePatchForm(String patch, Integer setNumber, LocalDateTime startsAt) {
		this.patch = patch;
		this.setNumber = setNumber;
		this.startsAt = startsAt;
	}
}
