package com.raishxn.modernspace.common;

import com.raishxn.modernspace.ModernSpace;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.MissingMappingsEvent;
import net.minecraftforge.registries.RegisterEvent;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;

/**
 * Imports a world that used the PersonalSpace built into the GregTech Nexus Addon (GTNA, up to 0.6.x).
 *
 * <p>
 * GTNA and ModernSpace share the same code and data format; only the namespace and the saved-data file name differ:
 * dimensions {@code gtna:personal_space/pdim_N}, dimension type {@code gtna:personal_space}, saved data
 * {@code data/gtna_personal_space.dat} and the portal {@code gtna:personal_space_portal}.
 *
 * <p>
 * {@link #migrateWorld(Path)} runs when Minecraft opens the world folder, before {@code level.dat} is parsed (the
 * old dimensions are stored there by Infiniverse, so vanilla would otherwise fail on the unknown generator). It
 * backs up every file it rewrites, moves the dimension folders, imports the saved data and rewrites the dimension
 * ids in {@code level.dat} and the player files. A dimension whose ModernSpace id is already taken is left untouched
 * and logged. A marker file makes it run once per world. The portal is aliased in the registries
 * ({@link #onRegister}, {@link #onMissingMappings}) and portal block entities fix their stored target on load
 * ({@link #remapDimension}).
 *
 * <p>
 * Nothing runs while a GTNA build that still ships PersonalSpace is installed.
 */
public final class GtnaMigration {

    private static final String OLD_NAMESPACE = "gtna";
    private static final String OLD_PREFIX = "gtna:personal_space";
    private static final String NEW_PREFIX = ModernSpace.MOD_ID + ":personal_space";
    private static final String PORTAL = "personal_space_portal";
    private static final String MARKER = "modernspace_gtna_migration.done";
    private static final String BACKUP_SUFFIX = ".modernspace_backup";
    private static final String GTNA_PERSONAL_SPACE_CLASS =
            "com.raishxn.gtna.common.world.personalspace.PersonalSpacePortalRegistry";

    private GtnaMigration() {}

    /** Whether an installed GTNA still registers its own PersonalSpace (then nothing is migrated or aliased). */
    public static boolean gtnaStillShipsPersonalSpace() {
        try {
            Class.forName(GTNA_PERSONAL_SPACE_CLASS, false, GtnaMigration.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /** {@code gtna:personal_space/pdim_N} becomes {@code modernspace:personal_space/pdim_N}; other ids are kept. */
    public static ResourceLocation remapDimension(ResourceLocation dimension) {
        if (OLD_NAMESPACE.equals(dimension.getNamespace()) && dimension.getPath().startsWith("personal_space/")) {
            return ModernSpace.id(dimension.getPath());
        }
        return dimension;
    }

    /** Old portal block, item and block entity ids resolve to the ModernSpace portal. */
    public static void onRegister(RegisterEvent event) {
        if (gtnaStillShipsPersonalSpace()) return;
        ResourceLocation from = ResourceLocation.fromNamespaceAndPath(OLD_NAMESPACE, PORTAL);
        ResourceLocation to = ModernSpace.id(PORTAL);
        if (event.getRegistryKey().equals(Registries.BLOCK)) {
            ((ForgeRegistry<?>) ForgeRegistries.BLOCKS).addAlias(from, to);
        } else if (event.getRegistryKey().equals(Registries.ITEM)) {
            ((ForgeRegistry<?>) ForgeRegistries.ITEMS).addAlias(from, to);
        } else if (event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)) {
            ((ForgeRegistry<?>) ForgeRegistries.BLOCK_ENTITY_TYPES).addAlias(from, to);
        }
    }

    /** Remaps the saved registry ids of the GTNA portal (block, item, block entity) on world load. */
    public static void onMissingMappings(MissingMappingsEvent event) {
        if (gtnaStillShipsPersonalSpace()) return;
        remap(event, Registries.BLOCK, PersonalSpacePortalRegistry.PORTAL.get());
        remap(event, Registries.ITEM, PersonalSpacePortalRegistry.PORTAL_ITEM.get());
        remap(event, Registries.BLOCK_ENTITY_TYPE, PersonalSpacePortalRegistry.PORTAL_ENTITY.get());
    }

    private static <T> void remap(MissingMappingsEvent event, ResourceKey<? extends Registry<T>> registry, T portal) {
        for (MissingMappingsEvent.Mapping<T> mapping : event.getMappings(registry, OLD_NAMESPACE)) {
            if (mapping.getKey().getPath().equals(PORTAL)) mapping.remap(portal);
        }
    }

    /** Imports a GTNA PersonalSpace world in place. Errors are logged and never stop the world from opening. */
    public static void migrateWorld(Path world) {
        try {
            if (Files.exists(world.resolve(MARKER)) || gtnaStillShipsPersonalSpace()) return;
            Path oldDimensions = world.resolve("dimensions").resolve(OLD_NAMESPACE).resolve("personal_space");
            Path oldData = world.resolve("data").resolve("gtna_personal_space.dat");
            Path levelDat = world.resolve("level.dat");
            if (!Files.isDirectory(oldDimensions) && !Files.exists(oldData) && !mentionsOldIds(levelDat)) return;

            ModernSpace.LOGGER.info("Importing GTNA PersonalSpace data into ModernSpace for {}", world);
            Set<Integer> skipped = new HashSet<>();
            moveDimensions(oldDimensions, world.resolve("dimensions").resolve(ModernSpace.MOD_ID)
                    .resolve("personal_space"), skipped);
            importDirectory(oldData, world.resolve("data").resolve("personal_space.dat"), skipped);
            rewrite(levelDat, skipped);
            Path players = world.resolve("playerdata");
            if (Files.isDirectory(players)) {
                try (DirectoryStream<Path> files = Files.newDirectoryStream(players, "*.dat")) {
                    for (Path file : files) rewrite(file, skipped);
                }
            }
            Files.writeString(world.resolve(MARKER), "GTNA PersonalSpace imported; skipped ids: " + skipped + "\n");
            ModernSpace.LOGGER.info("GTNA PersonalSpace import finished; skipped dimension ids: {}", skipped);
        } catch (Exception e) {
            ModernSpace.LOGGER.error("GTNA PersonalSpace import failed for {}; the originals are kept", world, e);
        }
    }

    private static boolean mentionsOldIds(Path levelDat) throws IOException {
        if (!Files.exists(levelDat)) return false;
        return containsOld(NbtIo.readCompressed(levelDat.toFile()));
    }

    private static boolean containsOld(Tag tag) {
        if (tag instanceof StringTag string) return isOld(string.getAsString());
        if (tag instanceof CompoundTag compound) {
            for (String key : compound.getAllKeys()) {
                if (isOld(key) || containsOld(compound.get(key))) return true;
            }
        } else if (tag instanceof ListTag list) {
            for (Tag element : list) if (containsOld(element)) return true;
        }
        return false;
    }

    /** {@code gtna:personal_space} (dimension type, generator) or {@code gtna:personal_space/...} (dimension). */
    private static boolean isOld(String value) {
        return value.equals(OLD_PREFIX) || value.startsWith(OLD_PREFIX + "/");
    }

    private static int dimensionId(String value) {
        int index = value.lastIndexOf("pdim_");
        if (index < 0) return -1;
        try {
            return Integer.parseInt(value.substring(index + 5));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static void moveDimensions(Path from, Path to, Set<Integer> skipped) throws IOException {
        if (!Files.isDirectory(from)) return;
        Files.createDirectories(to);
        try (DirectoryStream<Path> dimensions = Files.newDirectoryStream(from)) {
            for (Path dimension : dimensions) {
                Path target = to.resolve(dimension.getFileName().toString());
                if (Files.exists(target)) {
                    skipped.add(dimensionId(dimension.getFileName().toString()));
                    ModernSpace.LOGGER.warn("ModernSpace already has {}; the GTNA copy stays at {}", target,
                            dimension);
                    continue;
                }
                Files.move(dimension, target);
            }
        }
    }

    private static void importDirectory(Path oldData, Path newData, Set<Integer> skipped) throws IOException {
        if (!Files.exists(oldData)) return;
        CompoundTag old = NbtIo.readCompressed(oldData.toFile());
        CompoundTag oldRoot = old.getCompound("data");
        if (!Files.exists(newData)) {
            Files.copy(oldData, newData);
            return;
        }
        backup(newData);
        CompoundTag current = NbtIo.readCompressed(newData.toFile());
        CompoundTag root = current.getCompound("data");
        ListTag dimensions = root.getList("Dimensions", Tag.TAG_COMPOUND);
        Set<Integer> taken = new HashSet<>();
        for (int i = 0; i < dimensions.size(); i++) taken.add(dimensions.getCompound(i).getInt("Id"));
        ListTag oldDimensions = oldRoot.getList("Dimensions", Tag.TAG_COMPOUND);
        for (int i = 0; i < oldDimensions.size(); i++) {
            CompoundTag entry = oldDimensions.getCompound(i);
            if (taken.contains(entry.getInt("Id"))) {
                skipped.add(entry.getInt("Id"));
                continue;
            }
            dimensions.add(entry.copy());
        }
        root.put("Dimensions", dimensions);
        root.putInt("NextId", Math.max(root.getInt("NextId"), oldRoot.getInt("NextId")));
        current.put("data", root);
        NbtIo.writeCompressed(current, newData.toFile());
    }

    private static void rewrite(Path file, Set<Integer> skipped) throws IOException {
        if (!Files.exists(file)) return;
        CompoundTag tag = NbtIo.readCompressed(file.toFile());
        CompoundTag data = tag.getCompound("Data");
        boolean levelDat = file.getFileName().toString().equals("level.dat");
        // Only the world data and players: the root "fml" registry snapshot is handled by the aliases.
        Tag scope = levelDat ? data : tag;
        if (!containsOld(scope)) return;
        backup(file);
        Tag rewritten = replace(scope, skipped);
        if (levelDat) {
            // A skipped dimension keeps its folder but leaves level.dat: its gtna generator no longer exists.
            CompoundTag dimensions = ((CompoundTag) rewritten).getCompound("WorldGenSettings")
                    .getCompound("dimensions");
            for (String key : Set.copyOf(dimensions.getAllKeys())) {
                if (isOld(key)) dimensions.remove(key);
            }
            tag.put("Data", rewritten);
        } else {
            tag = (CompoundTag) rewritten;
        }
        NbtIo.writeCompressed(tag, file.toFile());
    }

    private static Tag replace(Tag tag, Set<Integer> skipped) {
        if (tag instanceof StringTag string) {
            String value = string.getAsString();
            return isOld(value) && !skipped.contains(dimensionId(value)) ?
                    StringTag.valueOf(NEW_PREFIX + value.substring(OLD_PREFIX.length())) : tag;
        }
        if (tag instanceof CompoundTag compound) {
            CompoundTag out = new CompoundTag();
            for (String key : compound.getAllKeys()) {
                String newKey = isOld(key) && !skipped.contains(dimensionId(key)) ?
                        NEW_PREFIX + key.substring(OLD_PREFIX.length()) : key;
                out.put(newKey, replace(compound.get(key), skipped));
            }
            return out;
        }
        if (tag instanceof ListTag list) {
            ListTag out = new ListTag();
            for (Tag element : list) out.add(replace(element, skipped));
            return out;
        }
        return tag;
    }

    private static void backup(Path file) throws IOException {
        Path copy = file.resolveSibling(file.getFileName() + BACKUP_SUFFIX);
        if (!Files.exists(copy)) Files.copy(file, copy, StandardCopyOption.COPY_ATTRIBUTES);
    }
}
