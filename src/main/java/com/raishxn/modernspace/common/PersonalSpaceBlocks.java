package com.raishxn.modernspace.common;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.item.ItemStack;
import net.minecraft.locale.Language;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Block names used by PersonalSpace presets. The 1.7.10 original stores {@code modid:name[:meta]}; GTNA stores
 * modern registry IDs and converts the legacy vanilla names and metadata that GTNH presets commonly use.
 */
public final class PersonalSpaceBlocks {

    private static final String[] COLORS = { "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink",
            "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black" };
    private static final String[] WOODS = { "oak", "spruce", "birch", "jungle", "acacia", "dark_oak" };
    private static final Map<String, String> LEGACY = new HashMap<>();

    static {
        for (int meta = 0; meta < 16; meta++) {
            LEGACY.put("minecraft:wool:" + meta, "minecraft:" + COLORS[meta] + "_wool");
            LEGACY.put("minecraft:stained_hardened_clay:" + meta, "minecraft:" + COLORS[meta] + "_terracotta");
            LEGACY.put("minecraft:stained_glass:" + meta, "minecraft:" + COLORS[meta] + "_stained_glass");
            LEGACY.put("minecraft:carpet:" + meta, "minecraft:" + COLORS[meta] + "_carpet");
        }
        for (int meta = 0; meta < WOODS.length; meta++) {
            LEGACY.put("minecraft:planks:" + meta, "minecraft:" + WOODS[meta] + "_planks");
        }
        LEGACY.put("minecraft:grass:0", "minecraft:grass_block");
        LEGACY.put("minecraft:double_stone_slab:0", "minecraft:smooth_stone");
        LEGACY.put("minecraft:hardened_clay:0", "minecraft:terracotta");
        LEGACY.put("minecraft:snow:0", "minecraft:snow_block");
        LEGACY.put("minecraft:brick_block:0", "minecraft:bricks");
        LEGACY.put("minecraft:stonebrick:0", "minecraft:stone_bricks");
        LEGACY.put("minecraft:stonebrick:1", "minecraft:mossy_stone_bricks");
        LEGACY.put("minecraft:stonebrick:2", "minecraft:cracked_stone_bricks");
        LEGACY.put("minecraft:stonebrick:3", "minecraft:chiseled_stone_bricks");
        LEGACY.put("minecraft:sand:1", "minecraft:red_sand");
        LEGACY.put("minecraft:dirt:1", "minecraft:coarse_dirt");
        LEGACY.put("minecraft:dirt:2", "minecraft:podzol");
        LEGACY.put("minecraft:sandstone:1", "minecraft:chiseled_sandstone");
        LEGACY.put("minecraft:sandstone:2", "minecraft:smooth_sandstone");
        LEGACY.put("minecraft:quartz_block:1", "minecraft:chiseled_quartz_block");
        LEGACY.put("minecraft:quartz_block:2", "minecraft:quartz_pillar");
        LEGACY.put("minecraft:lit_pumpkin:0", "minecraft:jack_o_lantern");
        LEGACY.put("minecraft:web:0", "minecraft:cobweb");
        LEGACY.put("minecraft:stone_slab:0", "minecraft:smooth_stone_slab");
        LEGACY.put("minecraft:mycelium:0", "minecraft:mycelium");
        LEGACY.put("minecraft:end_stone:0", "minecraft:end_stone");
        LEGACY.put("minecraft:nether_brick:0", "minecraft:nether_bricks");
        LEGACY.put("minecraft:quartz_ore:0", "minecraft:nether_quartz_ore");
    }

    private PersonalSpaceBlocks() {}

    /**
     * Converts {@code modid:name}, {@code modid:name:meta} (legacy) or an empty string to a modern block ID. Unknown
     * legacy metadata yields {@code null}; an unknown modern ID is returned unchanged so callers can report it.
     */
    public static String normalize(String name) {
        if (name == null) return "";
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return "";
        int at = trimmed.indexOf(STATE_SEPARATOR);
        if (at >= 0) {
            String base = normalize(trimmed.substring(0, at));
            return base == null || base.isEmpty() ? base : base + trimmed.substring(at);
        }
        int first = trimmed.indexOf(':');
        int last = trimmed.lastIndexOf(':');
        if (first > 0 && last > first) {
            String meta = trimmed.substring(last + 1);
            if (meta.chars().allMatch(Character::isDigit) && !meta.isEmpty()) {
                String base = trimmed.substring(0, last);
                String mapped = LEGACY.get(base + ":" + Integer.parseInt(meta));
                if (mapped != null) return mapped;
                return Integer.parseInt(meta) == 0 ? normalize(base) : null;
            }
        }
        String mapped = LEGACY.get(trimmed + ":0");
        return mapped != null ? mapped : trimmed;
    }

    /** Separates a block ID from block state values: {@code gtceu:white_lamp@inverted=true+bloom=false}. */
    public static final char STATE_SEPARATOR = '@';

    /** GregTech CEu lamp properties offered as separate entries (same choices as the lamp items). */
    private static final String[] LAMP_PROPERTIES = { "inverted", "bloom", "lit" };

    /** @return the block for a modern or legacy name, or {@code null} when it does not exist. */
    public static Block block(String name) {
        String id = normalize(name);
        if (id == null || id.isEmpty()) return null;
        int at = id.indexOf(STATE_SEPARATOR);
        if (at >= 0) id = id.substring(0, at);
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) return null;
        return BuiltInRegistries.BLOCK.getOptional(location).orElse(null);
    }

    /** @return the configured state, or {@code null} for empty, air or unknown blocks. */
    public static BlockState solidState(String name) {
        Block block = block(name);
        if (block == null || block == Blocks.AIR) return null;
        return state(block, name);
    }

    /** Applies the {@code @prop=value+prop=value} suffix of a name; unknown properties or values are ignored. */
    public static BlockState state(Block block, String name) {
        BlockState state = block.defaultBlockState();
        int at = name == null ? -1 : name.indexOf(STATE_SEPARATOR);
        if (at < 0) return state;
        for (String pair : name.substring(at + 1).split("\\+")) {
            int eq = pair.indexOf('=');
            if (eq <= 0) continue;
            Property<?> property = block.getStateDefinition().getProperty(pair.substring(0, eq).trim());
            if (property != null) state = with(state, property, pair.substring(eq + 1).trim());
        }
        return state;
    }

    private static <T extends Comparable<T>> BlockState with(BlockState state, Property<T> property, String value) {
        return property.getValue(value).map(v -> state.setValue(property, v)).orElse(state);
    }

    /** @return whether the block is a GregTech CEu lamp (inverted, bloom and lit flags). */
    public static boolean isLamp(Block block) {
        var definition = block.getStateDefinition();
        for (String name : LAMP_PROPERTIES) {
            if (!(definition.getProperty(name) instanceof BooleanProperty)) return false;
        }
        return "gtceu".equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace());
    }

    /** All eight lamp variants, the plain ID first (normal lamp with light and bloom). */
    public static List<String> lampVariants(String id) {
        List<String> result = new ArrayList<>();
        for (int mask = 0; mask < 8; mask++) {
            boolean inverted = (mask & 1) != 0, noBloom = (mask & 2) != 0, noLight = (mask & 4) != 0;
            List<String> parts = new ArrayList<>();
            if (inverted) parts.add("inverted=true");
            if (noBloom) parts.add("bloom=false");
            if (noLight) parts.add("lit=false");
            result.add(parts.isEmpty() ? id : id + STATE_SEPARATOR + String.join("+", parts));
        }
        return result;
    }

    /** Item shown for a name; GregTech lamps keep their variant in the item NBT. */
    public static ItemStack displayStack(String name) {
        Block block = block(name);
        if (block == null) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(block.asItem());
        if (!stack.isEmpty() && isLamp(block)) {
            BlockState state = state(block, name);
            for (String property : LAMP_PROPERTIES) {
                stack.getOrCreateTag().putBoolean(property,
                        state.getValue((BooleanProperty) block.getStateDefinition().getProperty(property)));
            }
        }
        return stack;
    }

    /** Human readable suffix for a lamp variant, e.g. {@code (inverted, no bloom)}. */
    public static String variantSuffix(String name) {
        Block block = block(name);
        if (block == null || !isLamp(block) || name.indexOf(STATE_SEPARATOR) < 0) return "";
        BlockState state = state(block, name);
        List<String> parts = new ArrayList<>();
        if (state.getValue((BooleanProperty) block.getStateDefinition().getProperty("inverted")))
            parts.add(Language.getInstance().getOrDefault("gui.personalWorld.lamp.inverted"));
        if (!state.getValue((BooleanProperty) block.getStateDefinition().getProperty("lit")))
            parts.add(Language.getInstance().getOrDefault("gui.personalWorld.lamp.noLight"));
        if (!state.getValue((BooleanProperty) block.getStateDefinition().getProperty("bloom")))
            parts.add(Language.getInstance().getOrDefault("gui.personalWorld.lamp.noBloom"));
        return parts.isEmpty() ? "" : " (" + String.join(", ", parts) + ")";
    }

    /** Ore blocks are never allowed in a personal dimension, whatever the config says. */
    public static boolean isOre(Block block) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
        String path = key.getPath();
        if (path.endsWith("_ore") || path.endsWith("ores") || path.contains("_ore_")) return true;
        if (block.defaultBlockState().is(ORES)) return true;
        return false;
    }

    private static final net.minecraft.tags.TagKey<Block> ORES = net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("forge", "ores"));

    public static boolean isAir(String name) {
        Block block = block(name);
        return block == Blocks.AIR || block == Blocks.CAVE_AIR || block == Blocks.VOID_AIR;
    }

    public static String id(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    /** Parses the original meta syntax: {@code 0}, {@code 0-12}, {@code !5}, {@code 0-15,!3}. */
    public static List<Integer> metaValues(String spec) {
        List<int[]> positive = new ArrayList<>();
        List<int[]> negative = new ArrayList<>();
        for (String partRaw : spec.split(",")) {
            String part = partRaw.trim();
            boolean negated = part.startsWith("!");
            if (negated) part = part.substring(1).trim();
            if (part.isEmpty()) continue;
            int[] range;
            try {
                int dash = part.indexOf('-');
                if (dash >= 0) {
                    int a = Math.max(0, Integer.parseInt(part.substring(0, dash).trim()));
                    int b = Math.max(0, Integer.parseInt(part.substring(dash + 1).trim()));
                    range = new int[] { Math.min(a, b), Math.max(a, b) };
                } else {
                    int m = Math.max(0, Integer.parseInt(part));
                    range = new int[] { m, m };
                }
            } catch (NumberFormatException exception) {
                return List.of();
            }
            (negated ? negative : positive).add(range);
        }
        if (positive.isEmpty() && negative.isEmpty()) positive.add(new int[] { 0, 0 });
        int max = 15;
        for (int[] range : positive) max = Math.max(max, Math.min(range[1], 255));
        List<Integer> result = new ArrayList<>();
        for (int meta = 0; meta <= max; meta++) {
            boolean allowed = positive.isEmpty();
            for (int[] range : positive) allowed |= meta >= range[0] && meta <= range[1];
            for (int[] range : negative) allowed &= meta < range[0] || meta > range[1];
            if (allowed) result.add(meta);
        }
        return result;
    }
}
