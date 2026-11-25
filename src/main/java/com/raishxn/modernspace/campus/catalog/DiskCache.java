package com.raishxn.modernspace.campus.catalog;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.raishxn.modernspace.ModernSpace;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.TreeSet;

/**
 * The machine catalog saved in {@code config/modernspace/}. Building it reads every multiblock preview (tens of
 * seconds with large packs), so it is built once and reused until a mod jar changes.
 */
final class DiskCache {
    private DiskCache() {}

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("modernspace").resolve("campus_catalog.json");
    }

    /** Hash of every mod file (name, size, modification time) and the catalog format. */
    private static String key(int format) {
        try {
            TreeSet<String> parts = new TreeSet<>();
            for (var info : ModList.get().getModFiles()) {
                Path path = info.getFile().getFilePath();
                parts.add(path.getFileName() + ":" + Files.size(path) + ":" + Files.getLastModifiedTime(path).toMillis());
            }
            var digest = MessageDigest.getInstance("SHA-256");
            digest.update(("format" + format).getBytes(StandardCharsets.UTF_8));
            for (String part : parts) digest.update(part.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            return null;
        }
    }

    static JsonObject load(int format) {
        try {
            String key = key(format);
            if (key == null || !Files.isRegularFile(file())) return null;
            JsonObject root = JsonParser.parseString(Files.readString(file())).getAsJsonObject();
            if (!key.equals(root.get("key").getAsString())) return null;
            ModernSpace.LOGGER.info("[campus] Machine catalog loaded from {}", file());
            return root.getAsJsonObject("catalog");
        } catch (Exception e) {
            return null;
        }
    }

    static void save(int format, JsonObject catalog) {
        try {
            String key = key(format);
            if (key == null || catalog.getAsJsonArray("reservations").isEmpty()) return;
            JsonObject root = new JsonObject();
            root.addProperty("key", key);
            root.add("catalog", catalog);
            Files.createDirectories(file().getParent());
            Files.writeString(file(), root.toString());
        } catch (Exception e) {
            ModernSpace.LOGGER.warn("[campus] Could not save the machine catalog: {}", e.toString());
        }
    }
}
