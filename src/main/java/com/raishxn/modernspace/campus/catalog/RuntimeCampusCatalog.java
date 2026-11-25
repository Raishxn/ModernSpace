package com.raishxn.modernspace.campus.catalog;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.raishxn.modernspace.ModernSpace;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Machine catalog used by the campus presets.
 * EN: built from the GregTech multiblocks when a GTCEu build with the multiblockpro structure API (GTO) is
 *     loaded, or from each multiblock's preview shape on upstream GTCEu; without GTCEu it is empty and the
 *     campus keeps its streets, halls and landscape without machine plots.
 * PT: montado a partir dos multiblocos do GregTech quando há um GTCEu com a API multiblockpro (GTO), ou da forma
 *     de prévia de cada multibloco no GTCEu normal; sem GTCEu fica vazio e o campus mantém ruas, halls e paisagem, sem lotes de máquina.
 * ZH: 加载带 multiblockpro 结构 API 的 GTCEu（GTO）时由格雷多方块生成，原版 GTCEu 下取各多方块的预览结构；没有 GTCEu 时为空，园区只保留道路、厂房和景观。
 */
public final class RuntimeCampusCatalog {
    private static volatile JsonObject empty;
    private static volatile Boolean available;
    /** Java 21 bridge in the gto source set; reached reflectively so the Java 17 code never links GregTech. */
    private static final String BRIDGE = "com.raishxn.modernspace.campus.catalog.GtoMachineCatalog";
    private static final int FORMAT = 2;

    private RuntimeCampusCatalog() {}

    /** True when the GregTech structure API needed for machine plots is present. */
    public static boolean gregTechAvailable() {
        Boolean result = available;
        if (result == null) {
            boolean loaded = Runtime.version().feature() >= 21 && ModList.get() != null && ModList.get().isLoaded("gtceu");
            if (loaded) {
                try {
                    Class.forName("com.gregtechceu.gtceu.api.machine.multiblockpro.Structure", false,
                            RuntimeCampusCatalog.class.getClassLoader());
                    Class.forName(BRIDGE, false, RuntimeCampusCatalog.class.getClassLoader());
                } catch (Throwable missing) {
                    loaded = false;
                    ModernSpace.LOGGER.info("[campus] GTCEu without the multiblockpro API: campus presets have no machine plots");
                }
            }
            available = result = loaded;
        }
        return result;
    }

    public static JsonObject json() {
        if (gregTechAvailable()) {
            try {
                return (JsonObject) bridge("json");
            } catch (Throwable error) {
                ModernSpace.LOGGER.error("[campus] GregTech machine catalog failed; using an empty catalog", error);
                available = false;
            }
        }
        if (upstreamAvailable()) return GtceuMachineCatalog.json();
        return emptyCatalog();
    }

    private static volatile Boolean upstream;

    /** Upstream GTCEu (7.x): machine plots from preview shapes instead of the multiblockpro API. */
    private static boolean upstreamAvailable() {
        Boolean result = upstream;
        if (result == null) {
            result = ModList.get() != null && ModList.get().isLoaded("gtceu") && GtceuMachineCatalog.available();
            if (result) ModernSpace.LOGGER.info("[campus] Upstream GTCEu: machine plots come from preview shapes");
            upstream = result;
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static List<String> skipped() {
        if (!gregTechAvailable()) return upstreamAvailable() ? GtceuMachineCatalog.skipped() : List.of();
        try { return (List<String>) bridge("skipped"); } catch (Throwable error) { return List.of(); }
    }

    private static Object bridge(String method) throws ReflectiveOperationException {
        return Class.forName(BRIDGE, true, RuntimeCampusCatalog.class.getClassLoader()).getMethod(method).invoke(null);
    }

    /** Writes the current catalog to config/modernspace/catalog-dump.json and returns the path. */
    public static Path dump() throws java.io.IOException {
        Path path = FMLPaths.CONFIGDIR.get().resolve(ModernSpace.MOD_ID).resolve("catalog-dump.json");
        Files.createDirectories(path.getParent());
        Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(json()), StandardCharsets.UTF_8);
        return path;
    }

    private static JsonObject emptyCatalog() {
        JsonObject result = empty;
        if (result == null) {
            result = new JsonObject();
            result.addProperty("format", FORMAT);
            result.addProperty("schemaVersion", 3);
            result.addProperty("registryHash", "none");
            result.add("reservations", new JsonArray());
            result.add("machines", new JsonArray());
            result.add("effectiveSharedIds", new JsonArray());
            result.add("skipped", new JsonArray());
            empty = result;
        }
        return result;
    }
}
