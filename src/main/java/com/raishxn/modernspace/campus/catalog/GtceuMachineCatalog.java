package com.raishxn.modernspace.campus.catalog;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.raishxn.modernspace.ModernSpace;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Campus machine catalog for upstream GTCEu (7.x), which has no multiblockpro structure API.
 * EN: sizes come from each multiblock's first preview shape (the same shape EMI/JEI shows). The GregTech API is
 *     reached reflectively because this jar compiles against the GTO fork, whose signatures differ.
 * PT: os tamanhos vêm da primeira forma de prévia de cada multibloco (a mesma que o EMI/JEI mostra). A API do
 *     GregTech é usada por reflexão porque este jar compila contra o fork do GTO, que tem assinaturas diferentes.
 * ZH: 尺寸取自每个多方块的第一个预览结构（与 EMI/JEI 显示的相同）。由于本 jar 针对 GTO 分支编译、签名不同，
 *     这里通过反射调用格雷 API。
 */
public final class GtceuMachineCatalog {
    private static final int FORMAT = 2, MAX_HEIGHT = 360, MAX_SIDE = 600;
    private static final int[] SHARED_MAX = {5, 7, 5};
    private static volatile JsonObject cached;
    private static final List<String> skipped = new ArrayList<>();

    private GtceuMachineCatalog() {}

    /** True when upstream GTCEu's preview API is on the classpath. */
    public static boolean available() {
        try {
            Class<?> def = Class.forName("com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition");
            def.getMethod("getMatchingShapes");
            Class.forName("com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo").getMethod("getBlocks");
            return true;
        } catch (Throwable missing) {
            return false;
        }
    }

    public static JsonObject json() {
        JsonObject result = cached;
        if (result != null) return result;
        synchronized (GtceuMachineCatalog.class) {
            if (cached == null) cached = build();
            return cached;
        }
    }

    public static synchronized List<String> skipped() { return List.copyOf(skipped); }

    private static JsonObject build() {
        JsonArray reservations = new JsonArray(), shared = new JsonArray(), skippedJson = new JsonArray();
        synchronized (GtceuMachineCatalog.class) { skipped.clear(); }
        long start = System.nanoTime();
        try {
            Class<?> multiClass = Class.forName("com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition");
            Object machines = Class.forName("com.gregtechceu.gtceu.api.registry.GTRegistries").getField("MACHINES").get(null);
            List<Object> definitions = new ArrayList<>();
            for (Object definition : (Iterable<?>) machines) if (multiClass.isInstance(definition)) definitions.add(definition);
            definitions.sort((a, b) -> id(a).compareTo(id(b)));
            for (Object definition : definitions) add(definition, reservations, shared, skippedJson);
        } catch (Throwable error) {
            ModernSpace.LOGGER.error("[campus] GTCEu machine catalog failed", error);
        }
        ModernSpace.LOGGER.info("[campus] Built GTCEu machine catalog: {} reservations, {} shared, {} skipped in {} ms",
                reservations.size(), shared.size(), skippedJson.size(), (System.nanoTime() - start) / 1_000_000);
        JsonObject root = new JsonObject();
        root.addProperty("format", FORMAT);
        root.addProperty("schemaVersion", 3);
        root.addProperty("registryHash", "gtceu-preview");
        root.addProperty("coordinateSystem", "LEFT/UP/FRONT; controller facing north (front=NORTH, up=NORTH, unflipped)");
        root.addProperty("generator", "modernspace GtceuMachineCatalog (first preview shape)");
        root.add("reservations", reservations);
        root.add("machines", reservations);
        root.add("effectiveSharedIds", shared);
        root.add("skipped", skippedJson);
        return root;
    }

    private static void add(Object definition, JsonArray reservations, JsonArray shared, JsonArray skippedJson) {
        String id = id(definition);
        try {
            int[] geometry = geometry(definition);
            if (geometry == null) { skip(skippedJson, id, "no preview"); return; }
            int w = geometry[0], h = geometry[1], d = geometry[2];
            if (h > MAX_HEIGHT || w > MAX_SIDE || d > MAX_SIDE) { skip(skippedJson, id, "too large " + w + "x" + h + "x" + d); return; }
            String text = keywords(definition, id);
            int tier = CampusHeuristics.tier((int) call(definition, "getTier"), text, w * h * d);
            String stage = tier <= 3 ? "early" : tier <= 8 ? "mid" : "late";
            boolean isShared = w <= SHARED_MAX[0] && h <= SHARED_MAX[1] && d <= SHARED_MAX[2];
            if (isShared) { shared.add(id); return; }
            JsonObject entry = new JsonObject();
            entry.addProperty("id", id);
            entry.addProperty("stage", stage);
            entry.addProperty("planningTier", tier);
            entry.addProperty("placementTier", tier);
            entry.add("size", array(w, h, d));
            entry.add("groundOffset", array(geometry[3], geometry[4], geometry[5]));
            entry.add("controllerForward", array(0, 0, -1));
            entry.addProperty("frontFacing", "north");
            entry.addProperty("copies", 1);
            entry.addProperty("theme", CampusHeuristics.theme((boolean) call(definition, "isGenerator"), text));
            entry.addProperty("patternResource", "runtime:" + id);
            entry.addProperty("allocation", "dedicated");
            entry.addProperty("displayNameKey", (String) call(definition, "getDescriptionId"));
            reservations.add(entry);
        } catch (Throwable error) {
            ModernSpace.LOGGER.warn("[campus] Preview size failed for {}: {}", id, error.toString());
            skip(skippedJson, id, "error: " + error);
        }
    }

    /**
     * {w, h, d, groundX, groundY, groundZ} with the controller turned to face north, or null without a preview.
     * EN: the side of the box the controller sits on is taken as its front; the box is rotated so that side is north.
     * PT: o lado da caixa onde fica o controlador é a frente; a caixa gira para esse lado ficar ao norte.
     * ZH: 控制器所在的包围盒一侧视为正面，旋转包围盒使该侧朝北。
     */
    static int[] geometry(Object definition) throws ReflectiveOperationException {
        List<?> shapes = (List<?>) call(definition, "getMatchingShapes");
        if (shapes == null || shapes.isEmpty()) return null;
        Object[][][] blocks = (Object[][][]) call(shapes.get(0), "getBlocks");
        if (blocks.length == 0 || blocks[0].length == 0 || blocks[0][0].length == 0) return null;
        Block controller = (Block) call(definition, "getBlock");
        int sx = blocks.length, sy = blocks[0].length, sz = blocks[0][0].length;
        int cx = sx / 2, cy = 0, cz = 0;
        Method stateOf = null;
        outer:
        for (int x = 0; x < sx; x++)
            for (int y = 0; y < sy; y++)
                for (int z = 0; z < sz; z++) {
                    Object info = blocks[x][y][z];
                    if (info == null) continue;
                    if (stateOf == null) stateOf = info.getClass().getMethod("getBlockState");
                    if (((BlockState) stateOf.invoke(info)).is(controller)) { cx = x; cy = y; cz = z; break outer; }
                }
        // Distances from the controller to the west, east, north and south sides.
        int west = cx, east = sx - 1 - cx, north = cz, south = sz - 1 - cz;
        int min = Math.min(Math.min(west, east), Math.min(north, south));
        if (min == north) return new int[] {sx, sy, sz, cx, cy, cz};
        if (min == south) return new int[] {sx, sy, sz, sx - 1 - cx, cy, sz - 1 - cz};
        if (min == west) return new int[] {sz, sy, sx, sz - 1 - cz, cy, cx};
        return new int[] {sz, sy, sx, cz, cy, sx - 1 - cx};
    }

    private static String keywords(Object definition, String id) {
        StringBuilder text = new StringBuilder(id.substring(id.indexOf(':') + 1));
        try {
            Object[] types = (Object[]) call(definition, "getRecipeTypes");
            if (types != null) for (Object type : types) if (type != null) text.append(' ').append(type);
        } catch (Throwable ignored) {
            // Keywords only tune the theme; the id alone is enough.
        }
        return text.toString().toLowerCase(Locale.ROOT);
    }

    private static String id(Object definition) {
        try { return ((ResourceLocation) call(definition, "getId")).toString(); } catch (Throwable e) { return String.valueOf(definition); }
    }

    private static Object call(Object target, String method) throws ReflectiveOperationException {
        return target.getClass().getMethod(method).invoke(target);
    }

    private static void skip(JsonArray out, String id, String reason) {
        JsonObject entry = new JsonObject(); entry.addProperty("id", id); entry.addProperty("reason", reason); out.add(entry);
        synchronized (GtceuMachineCatalog.class) { skipped.add(id + " (" + reason + ")"); }
    }

    private static JsonArray array(int... values) {
        JsonArray array = new JsonArray();
        for (int value : values) array.add(value);
        return array;
    }
}
