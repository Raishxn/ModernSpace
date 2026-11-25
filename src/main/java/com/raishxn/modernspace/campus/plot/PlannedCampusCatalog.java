package com.raishxn.modernspace.campus.plot;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** Reads the campus catalog into machine reservations ordered by tier and group leader. */
final class PlannedCampusCatalog {
    record Machine(String id, String stage, int tier, int width, int height, int depth,
                   int cx, int cy, int cz, String pattern, int slotIndex, String theme, String displayNameKey,
                   String leader) {
        /** The machine its group is ordered and placed by: the leader for a module, itself otherwise. */
        String anchor() { return leader.isEmpty() ? id : leader; }
    }

    private PlannedCampusCatalog() {}

    static List<Machine> machines(JsonObject catalog) {
        List<Machine> result = new ArrayList<>();
        Set<String> ids = new LinkedHashSet<>();
        for (var element : catalog.getAsJsonArray("reservations")) {
            JsonObject json = element.getAsJsonObject();
            var size = json.getAsJsonArray("size");
            var anchor = json.getAsJsonArray("groundOffset");
            String id = json.get("id").getAsString(), stage = json.get("stage").getAsString();
            int w = size.get(0).getAsInt(), h = size.get(1).getAsInt(), d = size.get(2).getAsInt();
            int cx = anchor.get(0).getAsInt(), cy = anchor.get(1).getAsInt(), cz = anchor.get(2).getAsInt();
            if (!ids.add(id) || w < 1 || h < 1 || d < 1 || cx < 0 || cx >= w || cy < 0 || cy >= h || cz < 0 || cz >= d
                    || !json.get("frontFacing").getAsString().equals("north")) {
                throw new IllegalArgumentException("Invalid planned machine " + id);
            }
            int tier = json.get("placementTier").getAsInt();
            int copies = json.get("copies").getAsInt();
            if (copies < 1 || copies > 16 || tier < 0 || tier > 14) {
                throw new IllegalArgumentException("Invalid campus allocation " + id);
            }
            for (int slot = 0; slot < copies; slot++) {
                result.add(new Machine(id, stage, tier, w, h, d, cx, cy, cz,
                        json.get("patternResource").getAsString(), slot, json.get("theme").getAsString(),
                        json.has("displayNameKey") ? json.get("displayNameKey").getAsString() : "",
                        json.has("groupLeader") ? json.get("groupLeader").getAsString() : ""));
            }
        }
        // EN: A module takes its leader's tier, so the group is placed together, right after the leader.
        // PT: O módulo assume o tier da máquina principal, para o grupo ser posto junto, logo depois dela.
        // ZH: 模块采用主机的等级，使整组紧随主机一起放置。
        Map<String, Integer> leaderTier = new HashMap<>();
        for (Machine machine : result) if (machine.leader.isEmpty()) leaderTier.putIfAbsent(machine.id, machine.tier);
        result.replaceAll(m -> m.leader.isEmpty() || !leaderTier.containsKey(m.leader) ? m
                : new Machine(m.id, m.stage, leaderTier.get(m.leader), m.width, m.height, m.depth, m.cx, m.cy, m.cz,
                        m.pattern, m.slotIndex, m.theme, m.displayNameKey, m.leader));
        return List.copyOf(result);
    }

    static List<Machine> order(List<Machine> machines) {
        Map<String, Machine> byId = new HashMap<>();
        for (Machine machine : machines) byId.putIfAbsent(machine.id, machine);
        Function<Machine, Machine> anchor = m -> byId.getOrDefault(m.anchor(), m);
        return machines.stream().sorted(Comparator.comparingInt(Machine::tier)
                .thenComparing(Comparator.comparingInt((Machine m) -> (anchor.apply(m).width + 16) * (anchor.apply(m).depth + 20)).reversed())
                .thenComparing(Comparator.comparingInt((Machine m) -> Math.max(anchor.apply(m).width, anchor.apply(m).depth)).reversed())
                .thenComparing(Machine::anchor)
                .thenComparing(m -> m.leader.isEmpty() ? 0 : 1)
                .thenComparing(Machine::id).thenComparingInt(Machine::slotIndex)).toList();
    }
}
