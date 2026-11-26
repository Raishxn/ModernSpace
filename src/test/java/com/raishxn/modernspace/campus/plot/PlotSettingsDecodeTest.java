package com.raishxn.modernspace.campus.plot;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.raishxn.modernspace.campus.catalog.RuntimeCampusCatalog;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Decoding or constructing campus settings must never build the machine catalog (it took 38 s on the render thread). */
class PlotSettingsDecodeTest {
    @Test
    void decodingCampusSettingsDoesNotTouchTheCatalog() {
        PlotSettings campus = PlotSettings.plannedCampus();
        JsonElement json = PlotSettingsCodec.CODEC.encodeStart(JsonOps.INSTANCE, campus).getOrThrow(false, message -> {});
        int before = RuntimeCampusCatalog.requests();

        PlotSettings decoded = PlotSettingsCodec.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(false, message -> {});

        assertEquals(campus, decoded);
        assertEquals(before, RuntimeCampusCatalog.requests(), "decoding PlotSettings asked for the machine catalog");
    }

    @Test
    void constructingFactoryAndFileCampusSettingsDoesNotTouchTheCatalog() {
        int before = RuntimeCampusCatalog.requests();
        PlotSettings.factoryCampus();
        PlotSettings.plannedCampus().withLevels(-64, -50);
        assertEquals(before, RuntimeCampusCatalog.requests());
    }

    @Test
    void cheapValidationStillRunsInTheConstructor() {
        assertThrows(IllegalArgumentException.class, () -> PlotSettings.plannedCampus().withLevels(-64, -60));
    }
}
