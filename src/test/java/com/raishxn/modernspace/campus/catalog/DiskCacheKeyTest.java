package com.raishxn.modernspace.campus.catalog;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class DiskCacheKeyTest {
    private static final List<String> MODS = List.of("gtceu:7.5.3", "modernspace:0.3.2", "minecraft:1.20.1");
    private static final List<String> MACHINES = List.of("gtceu:large_boiler", "gtceu:electric_blast_furnace");

    @Test
    void keyIgnoresOrderAndFileTimestamps() {
        String key = DiskCache.keyOf(2, MODS, MACHINES);
        assertEquals(key, DiskCache.keyOf(2, List.of("minecraft:1.20.1", "gtceu:7.5.3", "modernspace:0.3.2"),
                List.of("gtceu:electric_blast_furnace", "gtceu:large_boiler")));
    }

    @Test
    void keyChangesWithFormatVersionsOrMachines() {
        String key = DiskCache.keyOf(2, MODS, MACHINES);
        assertNotEquals(key, DiskCache.keyOf(3, MODS, MACHINES));
        assertNotEquals(key, DiskCache.keyOf(2, List.of("gtceu:7.5.4", "modernspace:0.3.2", "minecraft:1.20.1"), MACHINES));
        assertNotEquals(key, DiskCache.keyOf(2, MODS, List.of("gtceu:large_boiler")));
        assertNotEquals(key, DiskCache.keyOf(2, MODS, List.of("gtceu:large_boiler", "gtceu:electric_blast_furnace", "gtceu:new_machine")));
    }
}
