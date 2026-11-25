package pl.kubit.tft_meta_hub.gamedata;

import java.time.Instant;

public record GamePatchView(long id, String patch, int setNumber, Instant startsAt) {
}
