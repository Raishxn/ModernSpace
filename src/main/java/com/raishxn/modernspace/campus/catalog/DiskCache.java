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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.TreeSet;

/**
 * The machine catalog saved in {@code config/modernspace/}. Building it reads every multiblock preview (tens of
 * seconds with large packs), so it is built once and reused until the mod versions or the multiblock list change.
 */
final class DiskCache {
    private DiskCache() {}

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("modernspace").resolve("campus_catalog.json");
    }

    /**
     * Stable cache key: mod ids with versions, the sorted multiblock machine ids and the catalog format.
     * EN: file sizes and mtimes were removed because dev jars are rebuilt constantly and every rebuild missed the cache
     *     (a 38 s rebuild on the render thread); the same mod versions with the same multiblocks give the same catalog.
     * PT: tamanho e data dos arquivos saíram porque os jars de desenvolvimento são recompilados o tempo todo e cada
     *     recompilação perdia o cache (38 s na thread de render); mesmas versões com os mesmos multiblocos dão o mesmo catálogo.
     * ZH: 去掉文件大小与修改时间，因为开发 jar 频繁重编译，每次都会使缓存失效（渲染线程上耗时 38 秒）；
     *     相同的模组版本与相同的多方块会得到相同的目录。
     */
    static String keyOf(int format, Collection<String> modEntries, Collection<String> machineIds) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            digest.update(("format" + format + "\n").getBytes(StandardCharsets.UTF_8));
            for (String mod : new TreeSet<>(modEntries)) digest.update(("mod:" + mod + "\n").getBytes(StandardCharsets.UTF_8));
            for (String id : new TreeSet<>(machineIds)) digest.update(("machine:" + id + "\n").getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException impossible) {
            return null;
        }
    }

    private static String key(int format, Collection<String> machineIds) {
        try {
            List<String> mods = new ArrayList<>();
            for (var info : ModList.get().getMods()) mods.add(info.getModId() + ":" + info.getVersion());
            return keyOf(format, mods, machineIds);
        } catch (Exception e) {
            return null;
        }
    }

    static JsonObject load(int format, Collection<String> machineIds) {
        try {
            String key = key(format, machineIds);
            if (key == null || !Files.isRegularFile(file())) return null;
            JsonObject root = JsonParser.parseString(Files.readString(file())).getAsJsonObject();
            if (!key.equals(root.get("key").getAsString())) return null;
            ModernSpace.LOGGER.info("[campus] Machine catalog loaded from {}", file());
            return root.getAsJsonObject("catalog");
        } catch (Exception e) {
            return null;
        }
    }

    static void save(int format, Collection<String> machineIds, JsonObject catalog) {
        try {
            String key = key(format, machineIds);
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
