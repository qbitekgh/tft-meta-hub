package pl.kubit.tft_meta_hub.tierlist;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Input for creating, editing and copying a tier list. A plain Java bean with Bean Validation
 * (mutable, because Spring MVC binds form fields through setters); it has no Thymeleaf-specific parts,
 * so a future REST controller can accept the same object as a JSON request body.
 */
@Getter
@Setter
@NoArgsConstructor
public class TierListForm {

	@NotBlank
	@Size(max = 100)
	private String title;

	@NotBlank
	@Pattern(regexp = "\\d{1,2}\\.\\d{1,2}[a-z]?", message = "must look like 16.3 or 16.3b")
	private String patch;

	@NotNull
	@Min(1)
	@Max(99)
	private Integer setNumber;

	public TierListForm(String title, String patch, Integer setNumber) {
		this.title = title;
		this.patch = patch;
		this.setNumber = setNumber;
	}
}
