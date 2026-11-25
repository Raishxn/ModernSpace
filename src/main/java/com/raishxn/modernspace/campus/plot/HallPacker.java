package com.raishxn.modernspace.campus.plot;

import com.google.gson.JsonObject;
import com.raishxn.modernspace.ModernSpace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Packs runtime catalog machines into the authored halls of a file preset (Quartz Works / Pink Dream).
 * EN: preference = same department+stage, then same stage, then same department, then any hall; machines that fit nowhere are logged and skipped.
 * PT: preferência = mesmo departamento+estágio, depois estágio, depois departamento, depois qualquer hall; o que não couber é logado e pulado.
 * ZH: 优先同部门同阶段，其次同阶段、同部门、任意厂房；放不下的机器记录日志并跳过。
 */
final class HallPacker {
    static final int GAP = 3; // free blocks kept around each machine footprint

    private HallPacker() {}

    private record Machine(String id, String stage, int tier, int w, int h, int d, int gx, int gy, int gz,
                           String theme, String pattern, int slot, String nameKey) {}
    private static final class Space {
        final FactoryArchitecture.Hall hall; List<PlannedCampus.Bounds> free;
        Space(FactoryArchitecture.Hall hall, PlannedCampus.Bounds production) { this.hall = hall; this.free = new ArrayList<>(List.of(production)); }
    }

    static List<PlannedCampus.Plot> pack(String preset, List<FactoryArchitecture.Hall> halls, Collection<JsonObject> catalog,
                                         Map<String, PlannedCampus.Bounds> production) {
        return pack(preset, halls, catalog, production, null);
    }

    /** As {@link #pack}, adding to {@code unplaced} the catalog entries no hall fits (one per missing copy). */
    static List<PlannedCampus.Plot> pack(String preset, List<FactoryArchitecture.Hall> halls, Collection<JsonObject> catalog,
                                         Map<String, PlannedCampus.Bounds> production, List<JsonObject> unplaced) {
        Map<String, JsonObject> byId = new java.util.HashMap<>();
        for (JsonObject json : catalog) byId.put(json.get("id").getAsString(), json);
        List<Machine> machines = new ArrayList<>();
        for (JsonObject json : catalog) {
            var size = json.getAsJsonArray("size"); var anchor = json.getAsJsonArray("groundOffset");
            int copies = json.has("copies") ? json.get("copies").getAsInt() : 1;
            for (int slot = 0; slot < copies; slot++)
                machines.add(new Machine(json.get("id").getAsString(), json.get("stage").getAsString(), json.get("planningTier").getAsInt(),
                        size.get(0).getAsInt(), size.get(1).getAsInt(), size.get(2).getAsInt(),
                        anchor.get(0).getAsInt(), anchor.get(1).getAsInt(), anchor.get(2).getAsInt(),
                        json.get("theme").getAsString(), json.get("patternResource").getAsString(), slot,
                        json.has("displayNameKey") ? json.get("displayNameKey").getAsString() : ""));
        }
        machines.sort(Comparator.comparingInt((Machine m) -> m.w * m.d).reversed().thenComparing(Machine::id).thenComparingInt(Machine::slot));
        List<Space> spaces = new ArrayList<>();
        for (var hall : halls) spaces.add(new Space(hall, production.getOrDefault(hall.id(), hall.productionBounds())));
        List<PlannedCampus.Plot> plots = new ArrayList<>();
        int skipped = 0;
        for (Machine machine : machines) {
            PlannedCampus.Plot placed = null;
            for (int rank = 0; rank < 4 && placed == null; rank++) {
                for (Space space : spaces) {
                    if (rank(space.hall, machine) != rank) continue;
                    if (!space.hall.outdoor() && machine.h + 2 > space.hall.clearHeight()) continue;
                    placed = place(space, machine);
                    if (placed != null) break;
                }
            }
            if (placed == null && unplaced != null) {
                unplaced.add(byId.get(machine.id));
            } else if (placed == null) {
                skipped++;
                ModernSpace.LOGGER.warn("[campus:{}] No hall fits {} ({}x{}x{}); skipped", preset, machine.id, machine.w, machine.h, machine.d);
            } else plots.add(placed);
        }
        ModernSpace.LOGGER.info("[campus:{}] Packed {} machines into {} halls, {} skipped", preset, plots.size(), halls.size(), skipped);
        return plots;
    }

    private static int rank(FactoryArchitecture.Hall hall, Machine machine) {
        boolean stage = hall.stage().startsWith(machine.stage), department = hall.department().equals(machine.theme);
        return stage && department ? 0 : stage ? 1 : department ? 2 : 3;
    }

    /** Bottom-left first fit over maximal free rectangles. */
    private static PlannedCampus.Plot place(Space space, Machine machine) {
        int w = machine.w + 2 * GAP, d = machine.d + 2 * GAP;
        PlannedCampus.Bounds best = null;
        for (var free : space.free) {
            if (free.width() < w || free.depth() < d) continue;
            var candidate = new PlannedCampus.Bounds(free.minX(), free.minZ(), free.minX() + w - 1, free.minZ() + d - 1);
            if (best == null || candidate.minZ() < best.minZ() || candidate.minZ() == best.minZ() && candidate.minX() < best.minX()) best = candidate;
        }
        if (best == null) return null;
        space.free = subtract(space.free, best);
        int x = best.minX() + GAP, z = best.minZ() + GAP;
        return new PlannedCampus.Plot(machine.id, machine.stage, x, z, machine.w, machine.h, machine.d,
                x + machine.gx, 1 + machine.gy, z + machine.gz, machine.pattern, machine.slot, machine.theme, machine.nameKey);
    }

    private static boolean intersects(PlannedCampus.Bounds a, PlannedCampus.Bounds b) {
        return a.minX() <= b.maxX() && a.maxX() >= b.minX() && a.minZ() <= b.maxZ() && a.maxZ() >= b.minZ();
    }

    private static List<PlannedCampus.Bounds> subtract(List<PlannedCampus.Bounds> free, PlannedCampus.Bounds used) {
        Set<PlannedCampus.Bounds> parts = new LinkedHashSet<>();
        for (var b : free) {
            if (!intersects(b, used)) { parts.add(b); continue; }
            if (used.minX() > b.minX()) parts.add(new PlannedCampus.Bounds(b.minX(), b.minZ(), used.minX() - 1, b.maxZ()));
            if (used.maxX() < b.maxX()) parts.add(new PlannedCampus.Bounds(used.maxX() + 1, b.minZ(), b.maxX(), b.maxZ()));
            if (used.minZ() > b.minZ()) parts.add(new PlannedCampus.Bounds(b.minX(), b.minZ(), b.maxX(), used.minZ() - 1));
            if (used.maxZ() < b.maxZ()) parts.add(new PlannedCampus.Bounds(b.minX(), used.maxZ() + 1, b.maxX(), b.maxZ()));
        }
        List<PlannedCampus.Bounds> result = new ArrayList<>(parts);
        result.removeIf(inner -> parts.stream().anyMatch(outer -> outer != inner && !outer.equals(inner)
                && outer.minX() <= inner.minX() && outer.minZ() <= inner.minZ() && outer.maxX() >= inner.maxX() && outer.maxZ() >= inner.maxZ()));
        return result;
    }

}
