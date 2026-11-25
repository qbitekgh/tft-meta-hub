package pl.kubit.tft_meta_hub.gamedata;

public record GameDataImportResult(int setNumber, ImportCounts champions, ImportCounts traits, ImportCounts items) {

	/**
	 * @param added   records that did not exist before the import
	 * @param updated existing records (matched by set number and api name) that were overwritten
	 */
	public record ImportCounts(int added, int updated) {
	}
}
