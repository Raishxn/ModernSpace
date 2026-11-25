package com.raishxn.modernspace.campus.plot;

import com.raishxn.modernspace.ModernSpace;
import com.raishxn.modernspace.campus.plot.PlannedCampus.Bounds;
import com.raishxn.modernspace.campus.plot.PlannedCampus.Street;
import com.raishxn.modernspace.campus.plot.PlannedCampus.Workshop;
import com.raishxn.modernspace.campus.plot.PlannedCampusCatalog.Machine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Packs ordered campus machines into street-connected blocks and returns the remaining garden spaces. */
final class PlannedCampusPacker {
    record Placement(Machine machine, Bounds court) {}
    private record Candidate(Bounds court, Bounds free, int radius, int taxi, int slack, int waste) {}
    private record Connection(Street street, int length, int orientation, int edge, int target) {}
    record Packing(List<Placement> placements, List<Street> streets, List<Bounds> residuals, List<Bounds> greenways,
                   int halfWidth, int halfDepth) {}

    private PlannedCampusPacker() {}

    static Bounds expand(Bounds bounds, int margin) {
        return new Bounds(bounds.minX() - margin, bounds.minZ() - margin, bounds.maxX() + margin, bounds.maxZ() + margin);
    }

    static Bounds streetBounds(Street street) {
        return street.vertical()
                ? new Bounds(street.axis() - street.halfWidth(), street.start(), street.axis() + street.halfWidth() - 1, street.end())
                : new Bounds(street.start(), street.axis() - street.halfWidth(), street.end(), street.axis() + street.halfWidth() - 1);
    }

    static boolean intersects(Bounds a, Bounds b) {
        return a.minX() <= b.maxX() && a.maxX() >= b.minX() && a.minZ() <= b.maxZ() && a.maxZ() >= b.minZ();
    }

    static boolean encloses(Bounds outer, Bounds inner) {
        return outer.minX() <= inner.minX() && outer.minZ() <= inner.minZ()
                && outer.maxX() >= inner.maxX() && outer.maxZ() >= inner.maxZ();
    }

    /** Maximal empty rectangles retain both dimensions; no machine borrows another machine's depth. */
    static List<Bounds> subtract(List<Bounds> free, Bounds used) {
        Set<Bounds> fragments = new LinkedHashSet<>();
        for (Bounds bounds : free) {
            if (!intersects(bounds, used)) { fragments.add(bounds); continue; }
            if (used.minX() > bounds.minX()) fragments.add(new Bounds(bounds.minX(), bounds.minZ(), used.minX() - 1, bounds.maxZ()));
            if (used.maxX() < bounds.maxX()) fragments.add(new Bounds(used.maxX() + 1, bounds.minZ(), bounds.maxX(), bounds.maxZ()));
            if (used.minZ() > bounds.minZ()) fragments.add(new Bounds(bounds.minX(), bounds.minZ(), bounds.maxX(), used.minZ() - 1));
            if (used.maxZ() < bounds.maxZ()) fragments.add(new Bounds(bounds.minX(), used.maxZ() + 1, bounds.maxX(), bounds.maxZ()));
        }
        List<Bounds> result = new ArrayList<>(fragments);
        result.removeIf(inner -> fragments.stream().anyMatch(outer -> !outer.equals(inner) && encloses(outer, inner)));
        return result;
    }

    /** Largest coherent gardens first; unlike maximal packing spaces, these rectangles never overlap. */
    static List<Bounds> gardenRectangles(List<Bounds> free) {
        List<Bounds> pending = new ArrayList<>(free), gardens = new ArrayList<>();
        Comparator<Bounds> order = Comparator.comparingInt((Bounds bounds) -> bounds.width() * bounds.depth()).reversed()
                .thenComparing(Comparator.comparingInt((Bounds bounds) -> Math.min(bounds.width(), bounds.depth())).reversed())
                .thenComparingInt(Bounds::minX).thenComparingInt(Bounds::minZ);
        while (!pending.isEmpty()) {
            Bounds garden = pending.stream().min(order).orElseThrow();
            gardens.add(garden);
            pending = subtract(pending, garden);
        }
        return List.copyOf(gardens);
    }

    private static void addStreet(List<Street> streets, Street street) {
        if (!streets.contains(street)) streets.add(street);
    }

    private static void serviceRing(List<Street> streets, Bounds court, int north, int side, int south) {
        addStreet(streets, new Street(false, court.minZ() + north / 2, court.minX(), court.maxX(), north / 2));
        addStreet(streets, new Street(true, court.minX() + side / 2, court.minZ(), court.maxZ(), side / 2));
        addStreet(streets, new Street(true, court.maxX() + 1 - side / 2, court.minZ(), court.maxZ(), side / 2));
        addStreet(streets, new Street(false, court.maxZ() + 1 - south / 2, court.minX(), court.maxX(), south / 2));
    }

    private static List<Street> backbone(int half, List<Workshop> pads) {
        List<Street> streets = new ArrayList<>();
        // Every boulevard is a 64-block public reservation: road, planted belts and four-block outer walks.
        for (int axis : new int[] {0, -400, 400}) {
            int roadHalf = axis == 0 ? 16 : 12;
            addStreet(streets, new Street(true, axis, -half, half - 1, roadHalf));
            addStreet(streets, new Street(false, axis, -half, half - 1, roadHalf));
            for (int edge : new int[] {axis - 30, axis + 30}) {
                addStreet(streets, new Street(true, edge, -half, half - 1, 2));
                addStreet(streets, new Street(false, edge, -half, half - 1, 2));
            }
        }
        for (int axis : new int[] {-half + 12, half - 12}) {
            addStreet(streets, new Street(true, axis, -half, half - 1, 12));
            addStreet(streets, new Street(false, axis, -half, half - 1, 12));
        }
        for (Workshop pad : pads) {
            int axis = pad.z() < 0 ? pad.z() + pad.depth() + 2 : pad.z() - 2;
            addStreet(streets, new Street(false, axis, Math.min(0, pad.x() - 4), Math.max(0, pad.x() + pad.width() + 3), 2));
        }
        serviceRing(streets, expand(PlannedCampus.aeCoreBounds(), 4), 4, 4, 4);
        for (Workshop pad : pads) serviceRing(streets, expand(new Bounds(pad.x(), pad.z(), pad.x() + pad.width() - 1, pad.z() + pad.depth() - 1), 4), 4, 4, 4);
        for (Bounds garden : PlannedCampus.aeGardenBounds()) serviceRing(streets, expand(garden, 4), 4, 4, 4);
        return streets;
    }

    private static List<Bounds> greenBuffers(int half) {
        List<Bounds> buffers = new ArrayList<>();
        for (int axis : new int[] {0, -400, 400}) {
            buffers.add(new Bounds(axis - 32, -half + 24, axis + 31, half - 25));
            buffers.add(new Bounds(-half + 24, axis - 32, half - 25, axis + 31));
        }
        return buffers;
    }

    private static List<Bounds> coreReservations(List<Workshop> pads) {
        List<Bounds> core = new ArrayList<>();
        core.add(expand(PlannedCampus.aeCoreBounds(), 4));
        for (Workshop pad : pads) core.add(expand(new Bounds(pad.x(), pad.z(), pad.x() + pad.width() - 1, pad.z() + pad.depth() - 1), 4));
        for (Bounds garden : PlannedCampus.aeGardenBounds()) core.add(expand(garden, 4));
        return core;
    }

    /** Progression changes the actual allowed radius, rather than leaving all machines next to AE. */
    private static int minimumRadius(int tier) {
        return switch (tier) {
            case 4 -> 160; case 5 -> 180; case 6 -> 200; case 7 -> 220; case 8 -> 240;
            case 9 -> 400; case 10 -> 450; case 11 -> 500; case 12 -> 570; case 13 -> 640; case 14 -> 700;
            default -> 0;
        };
    }

    private static boolean better(Candidate a, Candidate b) {
        if (b == null) return true;
        if (a.radius != b.radius) return a.radius < b.radius;
        if (a.taxi != b.taxi) return a.taxi < b.taxi;
        if (a.slack != b.slack) return a.slack < b.slack;
        if (a.waste != b.waste) return a.waste < b.waste;
        return a.court.minX() != b.court.minX() ? a.court.minX() < b.court.minX() : a.court.minZ() < b.court.minZ();
    }

    private static boolean better(Connection a, Connection b) {
        if (b == null) return true;
        if (a.length != b.length) return a.length < b.length;
        if (a.orientation != b.orientation) return a.orientation < b.orientation;
        if (a.edge != b.edge) return a.edge < b.edge;
        return a.target < b.target;
    }

    /** A free rectangle edge meets a previously connected court or road; join only the nearest such contact. */
    private static Connection connect(Bounds court, Bounds free, List<Street> streets) {
        Connection best = null;
        for (int side = 0; side < 4; side++) {
            boolean vertical = side < 2, low = side == 0 || side == 2;
            int edge = vertical ? (low ? court.minX() : court.maxX()) : low ? court.minZ() : court.maxZ();
            int freeEdge = vertical ? (low ? free.minX() : free.maxX()) : low ? free.minZ() : free.maxZ();
            if (edge != freeEdge) continue;
            int courtStart = vertical ? court.minZ() : court.minX(), courtEnd = vertical ? court.maxZ() : court.maxX();
            int freeStart = vertical ? free.minZ() : free.minX(), freeEnd = vertical ? free.maxZ() : free.maxX();
            for (Street street : streets) {
                Bounds road = streetBounds(street);
                int contact = vertical ? (low ? road.maxX() : road.minX()) : low ? road.maxZ() : road.minZ();
                if (contact != edge + (low ? -1 : 1)) continue;
                int start = Math.max(freeStart, vertical ? road.minZ() : road.minX());
                int end = Math.min(freeEnd, vertical ? road.maxZ() : road.maxX());
                if (start > end) continue;
                int target = Math.max(start, Math.min(end, courtStart));
                int origin = Math.max(courtStart, Math.min(courtEnd, target)), length = Math.abs(target - origin);
                Street branch = length == 0 ? null : new Street(vertical, edge + (low ? 2 : -1), Math.min(target, origin), Math.max(target, origin), 2);
                Connection candidate = new Connection(branch, length, vertical ? 0 : 1, edge, target);
                if (better(candidate, best)) best = candidate;
            }
        }
        if (best == null) throw new IllegalStateException("Campus court has no connected packing edge");
        return best;
    }

    static Packing pack(List<Machine> machines, List<Workshop> pads, int half) {
        List<Street> streets = backbone(half, pads);
        List<Bounds> buffers = greenBuffers(half), core = coreReservations(pads);
        List<Bounds> free = new ArrayList<>(List.of(new Bounds(-half + 24, -half + 24, half - 25, half - 25)));
        for (Bounds buffer : buffers) free = subtract(free, buffer);
        for (Street street : streets) free = subtract(free, streetBounds(street));
        for (Bounds reservation : core) free = subtract(free, reservation);
        List<Bounds> publicGreen = new ArrayList<>(buffers);
        for (Street street : streets) publicGreen = subtract(publicGreen, streetBounds(street));
        for (Bounds reservation : core) publicGreen = subtract(publicGreen, reservation);
        List<Placement> placements = new ArrayList<>();
        Map<String, Bounds> placedLeaders = new HashMap<>();
        for (Machine machine : machines) {
            int width = machine.width() + 16, depth = machine.depth() + 20;
            Candidate best = null;
            Bounds leaderCourt = machine.leader().isEmpty() ? null : placedLeaders.get(machine.leader());
            for (Bounds space : free) {
                if (width > space.width() || depth > space.depth()) continue;
                for (int x : new int[] {space.minX(), space.maxX() - width + 1}) for (int z : new int[] {space.minZ(), space.maxZ() - depth + 1}) {
                    int cx = Math.abs(2 * x + width - 1), cz = Math.abs(2 * z + depth - 1);
                    if (Math.max(cx, cz) < 2 * minimumRadius(machine.tier())) continue;
                    int radius = Math.max(cx, cz), taxi = cx + cz;
                    if (leaderCourt != null) {
                        // EN: A module goes as close to its leader as the free space allows.
                        // PT: O módulo fica o mais próximo possível da máquina principal dentro do espaço livre.
                        // ZH: 模块在可用空间内尽量靠近其主机放置。
                        radius = Math.abs(2 * x + width - 1 - (leaderCourt.minX() + leaderCourt.maxX()))
                                + Math.abs(2 * z + depth - 1 - (leaderCourt.minZ() + leaderCourt.maxZ()));
                    }
                    Candidate candidate = new Candidate(new Bounds(x, z, x + width - 1, z + depth - 1), space, radius, taxi,
                            Math.min(space.width() - width, space.depth() - depth), space.width() * space.depth() - width * depth);
                    if (better(candidate, best)) best = candidate;
                }
            }
            if (best == null) {
                ModernSpace.LOGGER.warn("[campus] No free court for {} (slot {}, {}x{}x{}); skipped",
                        machine.id(), machine.slotIndex(), machine.width(), machine.height(), machine.depth());
                continue;
            }
            Connection connection;
            try { connection = connect(best.court, best.free, streets); }
            catch (IllegalStateException error) {
                ModernSpace.LOGGER.warn("[campus] {} has no road access; skipped", machine.id());
                continue;
            }
            free = subtract(free, best.court);
            if (connection.street != null) {
                addStreet(streets, connection.street);
                free = subtract(free, streetBounds(connection.street));
            }
            serviceRing(streets, best.court, 4, 4, 4);
            placements.add(new Placement(machine, best.court));
            if (machine.leader().isEmpty()) placedLeaders.putIfAbsent(machine.id(), best.court);
        }
        List<Bounds> greenways = gardenRectangles(publicGreen), residuals = new ArrayList<>(greenways);
        residuals.addAll(gardenRectangles(free));
        return new Packing(List.copyOf(placements), List.copyOf(streets), List.copyOf(residuals), greenways, half, half);
    }

    static Packing packing(List<Machine> machines, List<Workshop> pads) {
        Packing packing = pack(PlannedCampusCatalog.order(machines), pads, 1000);
        if (packing != null) return packing;
        throw new IllegalStateException("Campus catalog cannot fit the 2000-block park island");
    }
}
