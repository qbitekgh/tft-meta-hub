package pl.kubit.tft_meta_hub.tierlist;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A tier list of compositions for one patch. It is the aggregate root: compositions and their units
 * are changed only through its methods, which enforce the rules
 * (status transitions, archived lists are read-only, composition validation).
 */
@Entity
@Table(name = "tier_list")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TierList {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String title;

	/** Game patch, e.g. "16.3". */
	private String patch;

	private int setNumber;

	@Enumerated(EnumType.STRING)
	private TierListStatus status;

	private Instant publishedAt;

	private Instant updatedAt;

	@OneToMany(mappedBy = "tierList", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Composition> compositions = new ArrayList<>();

	public static TierList createDraft(String title, String patch, int setNumber, Instant now) {
		TierList tierList = new TierList();
		tierList.title = title.strip();
		tierList.patch = patch.strip();
		tierList.setNumber = setNumber;
		tierList.status = TierListStatus.DRAFT;
		tierList.updatedAt = now;
		return tierList;
	}

	public void updateDetails(String title, String patch, Instant now) {
		requireEditable();
		this.title = title.strip();
		this.patch = patch.strip();
		this.updatedAt = now;
	}

	public void addComposition(CompositionContent content, Instant now) {
		requireEditable();
		compositions.add(Composition.create(this, content));
		this.updatedAt = now;
	}

	public void updateComposition(long compositionId, CompositionContent content, Instant now) {
		requireEditable();
		findComposition(compositionId).update(content);
		this.updatedAt = now;
	}

	public void removeComposition(long compositionId, Instant now) {
		requireEditable();
		compositions.remove(findComposition(compositionId));
		this.updatedAt = now;
	}

	/** Only a draft can be published. Archiving the previously published list is the caller's job. */
	public void publish(Instant now) {
		if (status != TierListStatus.DRAFT) {
			throw new TierListRuleException("Only a draft can be published.");
		}
		this.status = TierListStatus.PUBLISHED;
		this.publishedAt = now;
		this.updatedAt = now;
	}

	public void archive(Instant now) {
		if (status != TierListStatus.PUBLISHED) {
			throw new TierListRuleException("Only a published tier list can be archived.");
		}
		this.status = TierListStatus.ARCHIVED;
		this.updatedAt = now;
	}

	/**
	 * "Copy to new patch": a new draft with copies of all compositions. Allowed from any status,
	 * because copying an archived list is the way to reuse it.
	 */
	public TierList copyAsDraft(String newTitle, String newPatch, Instant now) {
		TierList copy = createDraft(newTitle, newPatch, setNumber, now);
		compositions.stream()
				.sorted(Composition.DISPLAY_ORDER)
				.forEach(composition -> copy.compositions.add(Composition.create(copy, composition.toContent())));
		return copy;
	}

	public boolean isEditable() {
		return status != TierListStatus.ARCHIVED;
	}

	public boolean isDeletable() {
		return status == TierListStatus.DRAFT;
	}

	/** Compositions in display order: by tier, then by position inside the tier. */
	public List<Composition> getCompositions() {
		return compositions.stream()
				.sorted(Composition.DISPLAY_ORDER)
				.toList();
	}

	private void requireEditable() {
		if (!isEditable()) {
			throw new TierListRuleException("An archived tier list cannot be changed. Copy it to a new patch instead.");
		}
	}

	private Composition findComposition(long compositionId) {
		return compositions.stream()
				.filter(composition -> Objects.equals(composition.getId(), compositionId))
				.findFirst()
				.orElseThrow(() -> new TierListNotFoundException(
						"Composition " + compositionId + " not found in tier list " + id));
	}
}
