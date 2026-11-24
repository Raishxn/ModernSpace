package com.raishxn.modernspace.campus.catalog;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblockpro.Layout;
import com.gregtechceu.gtceu.api.machine.multiblockpro.Structure;
import com.gregtechceu.gtceu.api.machine.multiblockpro.StructureTree;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.raishxn.modernspace.ModernSpace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/**
 * Builds the campus machine catalog at runtime from the GTO GregTech machine registry.
 * Output uses the original GTL-Enhancedcore catalog schema: LEFT/UP/FRONT, controller facing north,
 * size = world AABB, groundOffset = controller relative to the west/north/bottom corner.
 * EN: every structure option (size, module count) is maximized so module-extended machines
 *     (e.g. EBF/liquefaction modules) reserve their full footprint.
 * PT: toda opção da estrutura (tamanho, módulos) vai ao máximo, então multiblocos com módulos reservam o espaço completo.
 * ZH: 结构的所有可选项（尺寸、模块数量）取最大值，带模块的多方块会预留完整占地。
 */
public final class GtoMachineCatalog {
    public static final int FORMAT = 2; // 2: modules folded into hosts, space-only excluded
    private static final int MAX_HEIGHT = 360, MAX_SIDE = 600;
    private static final int[] SHARED_MAX = {5, 7, 5};
    private static volatile JsonObject cached;
    private static final List<String> skipped = new ArrayList<>();

    private GtoMachineCatalog() {}

    public static JsonObject json() {
        JsonObject result = cached;
        if (result != null) return result;
        synchronized (GtoMachineCatalog.class) {
            if (cached == null) cached = loadOrBuild();
            return cached;
        }
    }

    public static synchronized List<String> skipped() { return List.copyOf(skipped); }

    /** Writes the current catalog to config/modernspace/catalog-dump.json and returns the path. */
    public static Path dump() throws java.io.IOException {
        Path path = dir().resolve("catalog-dump.json");
        Files.createDirectories(path.getParent());
        Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(json()), StandardCharsets.UTF_8);
        return path;
    }

    private static Path dir() { return FMLPaths.CONFIGDIR.get().resolve(ModernSpace.MOD_ID); }

    private static JsonObject loadOrBuild() {
        List<MultiblockMachineDefinition> definitions = new ArrayList<>();
        for (MachineDefinition definition : GTRegistries.MACHINES)
            if (definition instanceof MultiblockMachineDefinition multi) definitions.add(multi);
        definitions.sort((a, b) -> a.getId().toString().compareTo(b.getId().toString()));
        String hash = hash(definitions);
        Path file = dir().resolve("catalog-" + hash.substring(0, 16) + ".json");
        try {
            if (Files.isRegularFile(file)) {
                var json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
                if (json.has("format") && json.get("format").getAsInt() == FORMAT) {
                    ModernSpace.LOGGER.info("[campus] Loaded cached machine catalog {} ({} reservations)", file.getFileName(),
                            json.getAsJsonArray("reservations").size());
                    return json;
                }
            }
        } catch (Exception error) {
            ModernSpace.LOGGER.warn("[campus] Ignoring unreadable catalog cache {}", file, error);
        }
        long start = System.nanoTime();
        JsonObject json = build(definitions, hash);
        ModernSpace.LOGGER.info("[campus] Built machine catalog: {} reservations, {} shared, {} skipped in {} ms",
                json.getAsJsonArray("reservations").size(), json.getAsJsonArray("effectiveSharedIds").size(),
                json.getAsJsonArray("skipped").size(), (System.nanoTime() - start) / 1_000_000);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, new Gson().toJson(json), StandardCharsets.UTF_8);
        } catch (Exception error) {
            ModernSpace.LOGGER.warn("[campus] Could not write catalog cache {}", file, error);
        }
        return json;
    }

    private static String hash(List<MultiblockMachineDefinition> definitions) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            digest.update(("format=" + FORMAT + ";").getBytes(StandardCharsets.UTF_8));
            for (String mod : List.of("gtceu", "gtocore", ModernSpace.MOD_ID))
                digest.update((mod + "=" + ModList.get().getModContainerById(mod)
                        .map(c -> c.getModInfo().getVersion().toString()).orElse("-") + ";").getBytes(StandardCharsets.UTF_8));
            for (var definition : definitions) digest.update((definition.getId() + "\n").getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
    }

    private static JsonObject build(List<MultiblockMachineDefinition> definitions, String hash) {
        JsonArray reservations = new JsonArray(), shared = new JsonArray(), skippedJson = new JsonArray();
        synchronized (GtoMachineCatalog.class) { skipped.clear(); }
        for (var definition : definitions) {
            String id = definition.getId().toString();
            try {
                if (!definition.getMountedOn().isEmpty()) {
                    skip(skippedJson, id, "module of " + definition.getMountedOn().stream().map(p -> p.getId().toString()).toList()); continue;
                }
                if (spaceOnly(definition)) { skip(skippedJson, id, "space-only (space station)"); continue; }
                int[] geometry = geometry(definition);
                if (geometry == null) { skip(skippedJson, id, "no structure"); continue; }
                int w = geometry[0], h = geometry[1], d = geometry[2];
                if (h > MAX_HEIGHT || w > MAX_SIDE || d > MAX_SIDE) {
                    skip(skippedJson, id, "too large " + w + "x" + h + "x" + d); continue;
                }
                String text = keywords(definition);
                int tier = CampusHeuristics.tier(definition.getTier(), text, w * h * d);
                String stage = tier <= 3 ? "early" : tier <= 8 ? "mid" : "late";
                boolean isShared = w <= SHARED_MAX[0] && h <= SHARED_MAX[1] && d <= SHARED_MAX[2];
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
                entry.addProperty("theme", CampusHeuristics.theme(definition.isGenerator(), text));
                entry.addProperty("patternResource", "runtime:" + id);
                entry.addProperty("allocation", isShared ? "shared" : "dedicated");
                entry.addProperty("displayNameKey", definition.getDescriptionId());
                var members = members(definition);
                if (!members.isEmpty()) {
                    JsonArray memberIds = new JsonArray(); memberIds.add(id); members.forEach(memberIds::add);
                    entry.add("memberIds", memberIds);
                }
                if (isShared) shared.add(id); else reservations.add(entry);
            } catch (Throwable error) {
                ModernSpace.LOGGER.warn("[campus] Pattern generation failed for {}: {}", id, error.toString());
                skip(skippedJson, id, "error: " + error);
            }
        }
        JsonObject root = new JsonObject();
        root.addProperty("format", FORMAT);
        root.addProperty("schemaVersion", 3);
        root.addProperty("registryHash", hash);
        root.addProperty("coordinateSystem", "LEFT/UP/FRONT; controller facing north (front=NORTH, up=NORTH, unflipped)");
        root.addProperty("generator", "modernspace GtoMachineCatalog (GTRegistries.MACHINES, max structure options)");
        root.add("reservations", reservations);
        root.add("machines", reservations);
        root.add("effectiveSharedIds", shared);
        root.add("skipped", skippedJson);
        return root;
    }

    private static void skip(JsonArray out, String id, String reason) {
        ModernSpace.LOGGER.info("[campus] Skipping {}: {}", id, reason);
        JsonObject entry = new JsonObject(); entry.addProperty("id", id); entry.addProperty("reason", reason); out.add(entry);
        synchronized (GtoMachineCatalog.class) { skipped.add(id + " (" + reason + ")"); }
    }

    private static JsonArray array(int... values) {
        JsonArray array = new JsonArray();
        for (int value : values) array.add(value);
        return array;
    }

    private static String keywords(MachineDefinition definition) {
        StringBuilder text = new StringBuilder(definition.getId().getPath());
        GTRecipeType[] types = definition.getRecipeTypes();
        if (types != null) for (GTRecipeType type : types) if (type != null) text.append(' ').append(type.registryName.getPath()).append(' ').append(type.group);
        return text.toString().toLowerCase(Locale.ROOT);
    }

    /** Returns {w,h,d, groundX, groundY, groundZ} or null when the definition has no buildable structure. */
    static int[] geometry(MultiblockMachineDefinition definition) { return geometry(definition, true); }

    private static int[] geometry(MultiblockMachineDefinition definition, boolean withModules) {
        Structure structure = definition.displayStructure();
        if (structure == null && definition.hasStructure()) structure = definition.getStructure();
        if (structure == null) return null;
        int[] values = maximize(structure);
        Layout layout = structure.layout(values);
        if (layout == null) layout = structure.layout(structure.defaultValues());
        if (layout == null) return null;
        int minX = 0, minY = 0, minZ = 0, maxX = 0, maxY = 0, maxZ = 0;
        var cursor = new BlockPos.MutableBlockPos();
        int count = layout.cells().size();
        for (int i = 0; i < count; i++) {
            var p = layout.worldPos(BlockPos.ZERO, i, Direction.NORTH, Direction.NORTH, false, cursor);
            minX = Math.min(minX, p.getX()); minY = Math.min(minY, p.getY()); minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX()); maxY = Math.max(maxY, p.getY()); maxZ = Math.max(maxZ, p.getZ());
        }
        // Modules (own controller, mountedOn a protocol) sit on MACHINES ports; add each module's maximal body
        // placed at the port, controller facing the host, body extending outward.
        // PT: módulos ficam nas portas MACHINES; soma o corpo máximo de cada módulo apontando para fora do host.
        // ZH: 模块挂在 MACHINES 端口上；把每个模块的最大结构朝外叠加到宿主包围盒。
        if (withModules) {
            var tree = structure.tree().nodes();
            for (int i = 0; i < count; i++) {
                int node = layout.cells().get(i).node();
                if (node < 0 || node >= tree.length || tree[node].kind() != StructureTree.Kind.MACHINES || tree[node].protocol() == null) continue;
                var port = layout.worldPos(BlockPos.ZERO, i, Direction.NORTH, Direction.NORTH, false).immutable();
                Direction out = layout.outwardWorld(i, Direction.NORTH, Direction.NORTH, false);
                if (out == null) out = Direction.NORTH;
                for (var member : tree[node].protocol().getMembers()) {
                    if (!(member instanceof MultiblockMachineDefinition module) || module == definition) continue;
                    int[] box = moduleBox(module, port, out.getOpposite());
                    if (box == null) continue;
                    minX = Math.min(minX, box[0]); minY = Math.min(minY, box[1]); minZ = Math.min(minZ, box[2]);
                    maxX = Math.max(maxX, box[3]); maxY = Math.max(maxY, box[4]); maxZ = Math.max(maxZ, box[5]);
                }
            }
        }
        return new int[] {maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1, -minX, -minY, -minZ};
    }

    /** World AABB {minX,minY,minZ,maxX,maxY,maxZ} of a module whose controller sits at {@code port} facing {@code front}. */
    private static int[] moduleBox(MultiblockMachineDefinition module, BlockPos port, Direction front) {
        try {
            Structure structure = module.displayStructure();
            if (structure == null && module.hasStructure()) structure = module.getStructure();
            if (structure == null) return null;
            Layout layout = structure.layout(maximize(structure));
            if (layout == null) return null;
            int[] b = {port.getX(), port.getY(), port.getZ(), port.getX(), port.getY(), port.getZ()};
            var cursor = new BlockPos.MutableBlockPos();
            for (int i = 0; i < layout.cells().size(); i++) {
                var p = layout.worldPos(port, i, front, Direction.NORTH, false, cursor);
                b[0] = Math.min(b[0], p.getX()); b[1] = Math.min(b[1], p.getY()); b[2] = Math.min(b[2], p.getZ());
                b[3] = Math.max(b[3], p.getX()); b[4] = Math.max(b[4], p.getY()); b[5] = Math.max(b[5], p.getZ());
            }
            return b;
        } catch (Throwable error) {
            ModernSpace.LOGGER.warn("[campus] Module {} geometry failed: {}", module.getId(), error.toString());
            return null;
        }
    }

    /** Module ids that mount on this host's MACHINES ports (used for search aliases). */
    private static List<String> members(MultiblockMachineDefinition definition) {
        List<String> ids = new ArrayList<>();
        Structure structure = definition.displayStructure();
        if (structure == null) return ids;
        for (var node : structure.tree().nodes())
            if (node.protocol() != null) for (var member : node.protocol().getMembers()) {
                String id = member.getId().toString();
                if (!ids.contains(id)) ids.add(id);
            }
        return ids;
    }

    /** Space stations and anything docking on station protocols only work in space — never placed on the campus. */
    private static boolean spaceOnly(MultiblockMachineDefinition definition) {
        if (definition.getId().getPath().contains("space_station")) return true;
        Structure structure = definition.displayStructure();
        if (structure == null) return false;
        for (var node : structure.tree().nodes())
            if (node.protocol() != null && node.protocol().getId().getPath().startsWith("station_")) return true;
        return false;
    }

    /** SIZE/COUNT options at their largest accepted value; CHOICE options at the choice with the largest footprint. */
    private static int[] maximize(Structure structure) {
        int[] values = structure.defaultValues();
        var options = structure.options();
        for (int i = 0; i < options.size(); i++) {
            var option = options.get(i);
            if (option.kind() == Structure.Option.Kind.CHOICE) {
                int best = values[i]; long bestArea = -1;
                for (int choice = option.min(); choice <= option.max(); choice++) {
                    int[] trial = values.clone(); trial[i] = choice;
                    if (!structure.accepts(trial)) continue;
                    long area = footprint(structure, trial);
                    if (area > bestArea) { bestArea = area; best = choice; }
                }
                values[i] = best;
            } else {
                for (int value = option.max(); value > values[i]; value--) {
                    int[] trial = values.clone(); trial[i] = value;
                    if (structure.accepts(trial)) { values[i] = value; break; }
                }
            }
        }
        return values;
    }

    private static long footprint(Structure structure, int[] values) {
        try {
            Layout layout = structure.layout(values);
            return layout == null ? -1 : (long) layout.width() * layout.depth() * 1000 + layout.height();
        } catch (Throwable error) {
            return -1;
        }
    }

}
