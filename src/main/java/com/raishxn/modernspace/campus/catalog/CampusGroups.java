package com.raishxn.modernspace.campus.catalog;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.raishxn.modernspace.ModernSpace;
import net.minecraftforge.fml.ModList;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Machines that work together and should stand side by side on the campus (a multiblock and its modules).
 * <p>
 * Any mod may ship {@code modernspace_campus_groups.json} at the root of its jar:
 * <pre>{"groups": [{"leader": "mod:main_machine", "members": ["mod:module_a", "mod:module_*"]}]}</pre>
 * A member ending in {@code *} matches every machine id with that prefix.
 */
final class CampusGroups {
    static final String FILE = "modernspace_campus_groups.json";

    private CampusGroups() {}

    /** Member id to leader id, for the machines in {@code ids}. Leaders map to nothing. */
    static Map<String, String> leaders(Collection<String> ids) {
        Map<String, String> result = new HashMap<>();
        for (var file : ModList.get().getModFiles()) {
            Path path = file.getFile().findResource(FILE);
            if (!Files.isRegularFile(path)) continue;
            try (Reader reader = Files.newBufferedReader(path)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                for (var element : root.getAsJsonArray("groups")) {
                    JsonObject group = element.getAsJsonObject();
                    String leader = group.get("leader").getAsString();
                    if (!ids.contains(leader)) continue;
                    for (var member : group.getAsJsonArray("members")) {
                        String pattern = member.getAsString();
                        for (String id : ids) {
                            if (id.equals(leader)) continue;
                            boolean match = pattern.endsWith("*") ? id.startsWith(pattern.substring(0, pattern.length() - 1))
                                    : id.equals(pattern);
                            if (match) result.putIfAbsent(id, leader);
                        }
                    }
                }
            } catch (Exception error) {
                ModernSpace.LOGGER.warn("[campus] Ignoring {} in {}: {}", FILE, file.getFile().getFileName(), error.toString());
            }
        }
        return result;
    }
}
