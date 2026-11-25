package pl.kubit.tft_meta_hub.gamedata;

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
import pl.kubit.tft_meta_hub.gamedata.GameData.ItemData;

@Entity
@Table(name = "item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Item {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private int setNumber;

	private String apiName;

	private String name;

	private String iconUrl;

	@Enumerated(EnumType.STRING)
	private ItemCategory category;

	public static Item create(int setNumber, ItemData data) {
		Item item = new Item();
		item.setNumber = setNumber;
		item.apiName = data.apiName();
		item.updateFrom(data);
		return item;
	}

	public void updateFrom(ItemData data) {
		this.name = data.name();
		this.iconUrl = data.iconUrl();
		this.category = data.category();
	}
}
