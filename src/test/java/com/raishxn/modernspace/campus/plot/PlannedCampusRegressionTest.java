package com.raishxn.modernspace.campus.plot;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Regression snapshot test verifying PlannedCampus layout remains identical after splitting. */
class PlannedCampusRegressionTest {
    private static final String CATALOG_RESOURCE = "/planned_campus_catalog.json";
    private static final String SNAPSHOT_RESOURCE = "/planned_campus_snapshot.txt";

    @Test
    void planMatchesSnapshot() throws Exception {
        JsonObject catalog = readJson(CATALOG_RESOURCE);
        List<PlannedCampus.Plot> plots = PlannedCampus.planPlots(catalog);
        assertFalse(plots.isEmpty(), "Expected sample catalog to produce plots");

        StringBuilder actual = new StringBuilder();
        for (PlannedCampus.Plot plot : plots) {
            actual.append(plot.id())
                    .append(' ').append(plot.x())
                    .append(' ').append(plot.z())
                    .append(' ').append(plot.width())
                    .append(' ').append(plot.depth())
                    .append('\n');
        }
        assertEquals(readText(SNAPSHOT_RESOURCE), actual.toString());
    }

    private static JsonObject readJson(String resource) throws Exception {
        try (var stream = PlannedCampusRegressionTest.class.getResourceAsStream(resource)) {
            assertNotNull(stream, "Missing " + resource);
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        }
    }

    private static String readText(String resource) throws Exception {
        try (var stream = PlannedCampusRegressionTest.class.getResourceAsStream(resource)) {
            assertNotNull(stream, "Missing " + resource);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
