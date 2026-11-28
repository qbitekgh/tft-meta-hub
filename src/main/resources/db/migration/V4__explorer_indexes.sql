-- Indexes for the explorer's statistics queries.

-- General parameters: the patch is always set, the time range is a lower bound.
-- The composite index also serves lookups by patch alone, so the single-column one from V3 is no longer needed.
CREATE INDEX ix_tft_match_patch_game_datetime ON tft_match (patch, game_datetime);
DROP INDEX ix_tft_match_patch;

-- Unit filters run EXISTS (... WHERE participant_id = ? AND character_id = ?) for every board.
CREATE INDEX ix_participant_unit_participant_character ON participant_unit (participant_id, character_id);

-- Builds and item holders look up all units of one champion.
CREATE INDEX ix_participant_unit_character_id ON participant_unit (character_id);

-- Item filters look for an item name; the primary key (unit_id, slot) already covers lookups by unit.
CREATE INDEX ix_participant_unit_item_item_name ON participant_unit_item (item_name);

-- Trait filters run EXISTS (... WHERE participant_id = ? AND name = ?) for every board.
CREATE INDEX ix_participant_trait_participant_name ON participant_trait (participant_id, name);
