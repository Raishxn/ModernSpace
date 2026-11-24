package com.raishxn.modernspace.campus.catalog;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Heuristic tier/theme classification (the original used offline recipe evidence we do not have).
 * PT: classificação heurística por tier da máquina, recipe types e palavras-chave do id.
 * ZH: 根据机器电压等级、配方类型和 ID 关键词进行启发式分类。
 */
public final class CampusHeuristics {
    private CampusHeuristics() {}

    private static final Pattern PRIMITIVE = Pattern.compile("steam|bronze|primitive|coke_oven|brick|wooden|stone_");
    private static final Pattern ENDGAME = Pattern.compile("eye_of_harmony|harmony|cosmic|galaxy|universe|infinity|godforge|forge_of_gods|"
            + "transcend|chaos|creation|eternity|dyson|singularity|black_hole|space_elevator|star_|stellar|neutron|quasar|dimension");
    private static final Pattern LATE = Pattern.compile("fusion|quantum|nano|pico|naquadah|plasma|antimatter|hyper|ultimate|super_");

    /** Order matters: first match wins. Theme names match the original preview palette. */
    private static final Map<String, Pattern> THEMES = new LinkedHashMap<>();
    static {
        THEMES.put("steam", PRIMITIVE);
        THEMES.put("space", Pattern.compile("space|orbit|rocket|stellar|star|cosmic|galaxy|universe|dimension|harmony|void|celestial|astral|dyson"));
        THEMES.put("energy", Pattern.compile("generator|turbine|boiler|fusion|fission|reactor_|nuclear|power|dynamo|substation|engine|naquadah_reactor|battery|energy"));
        THEMES.put("computation", Pattern.compile("comput|data|research|server|network|hpca|supercomputer|analy[sz]"));
        THEMES.put("biology", Pattern.compile("bio|bacteri|plant|greenhouse|farm|tree|incubat|ferment|dna|gene|crop|bee|brew|food|culture"));
        THEMES.put("ore", Pattern.compile("ore|mine|miner|drill|sift|wash|macerat|crush|grind|flotation|mining|pulveri|quarry|dig"));
        THEMES.put("chemistry", Pattern.compile("chem|distil|crack|refin|petro|mixer|electroly|centrifug|fluid|dehydrat|vacuum|liquef|gas|polymer|reactor|catal|solution|dissolv"));
        THEMES.put("metallurgy", Pattern.compile("blast|furnace|smelt|alloy|arc_|forge|metal|bender|extrud|press|wire|lathe|cutter|foundry|ingot|anneal|heat|coil|casting|mold"));
        THEMES.put("precision", Pattern.compile("assembl|circuit|laser|engrav|precision|litho|component|wafer|chip|fabricat"));
    }

    public static String theme(boolean generator, String text) {
        if (generator && !PRIMITIVE.matcher(text).find()) return "energy";
        for (var entry : THEMES.entrySet()) if (entry.getValue().matcher(text).find()) return entry.getKey();
        return "precision";
    }

    /** 0..14 (ULV..MAX). Uses the definition tier when set, otherwise keywords and volume. */
    public static int tier(int definitionTier, String text, int volume) {
        int tier = Math.max(0, Math.min(14, definitionTier));
        if (PRIMITIVE.matcher(text).find()) return Math.min(tier, 1);
        if (ENDGAME.matcher(text).find()) tier = Math.max(tier, 11);
        else if (LATE.matcher(text).find()) tier = Math.max(tier, 7);
        if (definitionTier <= 0) {
            int bySize = volume < 150 ? 2 : volume < 1500 ? 4 : volume < 15000 ? 6 : volume < 100000 ? 9 : 11;
            tier = Math.max(tier, bySize);
        }
        return tier;
    }
}
