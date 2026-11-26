package com.raishxn.modernspace.gametest;

import com.raishxn.modernspace.ModernSpace;
import com.raishxn.modernspace.campus.device.CampusDeviceBlocks;
import com.raishxn.modernspace.campus.plot.CampusPresetRegistry;
import com.raishxn.modernspace.campus.plot.PlotChunkGenerator;
import com.raishxn.modernspace.campus.plot.PlotPresetShare;
import com.raishxn.modernspace.campus.plot.PlotSettings;
import com.raishxn.modernspace.common.PersonalSpaceDirectory;
import com.raishxn.modernspace.common.PersonalSpaceSettings;
import com.raishxn.modernspace.common.PersonalSpaceWorlds;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * EN: plot and campus presets create a ModernSpace dimension with the plot generator, the overworld height and the
 *     central teleporter; runs without GregTech, so it also covers the empty machine catalog.
 * PT: presets de lotes e campus criam uma dimensão do ModernSpace com o gerador de lotes, a altura do overworld e o
 *     teleportador central; roda sem GregTech, então cobre também o catálogo vazio.
 * ZH: 地块与园区预设会创建使用地块生成器、主世界高度和中央传送器的 ModernSpace 维度；不加载格雷，也覆盖空机器目录。
 */
@GameTestHolder(ModernSpace.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CampusGameTests {

    private CampusGameTests() {}

    @GameTest(template = "qa_empty", batch = "plot_grid", timeoutTicks = 200)
    public static void gridPresetCreatesPlotDimension(GameTestHelper helper) {
        PlotSettings plot = PlotSettings.preset(4);
        ServerLevel level = create(helper, plot);
        BlockPos surface = new BlockPos(5, plot.surfaceY(), 5);
        helper.assertFalse(level.getBlockState(surface).isAir(), "grid surface is air at " + surface);
        helper.assertTrue(level.getBlockState(surface.above()).isAir(), "grid has a block above the surface");
        helper.succeed();
    }

    @GameTest(template = "qa_empty", batch = "plot_woodland", timeoutTicks = 400)
    public static void woodlandCampusHasCentralTeleporter(GameTestHelper helper) {
        PlotSettings plot = PlotSettings.plannedCampus();
        ServerLevel level = create(helper, plot);
        assertTeleporter(helper, level, plot);
        helper.succeed();
    }

    @GameTest(template = "qa_empty", batch = "plot_files", timeoutTicks = 1200)
    public static void fileCampusPresetsHaveCentralTeleporter(GameTestHelper helper) {
        for (var definition : CampusPresetRegistry.creationDefinitions()) {
            PlotSettings plot = PlotSettings.fileCampus(definition.id());
            ServerLevel level = create(helper, plot);
            assertTeleporter(helper, level, plot);
        }
        helper.succeed();
    }

    /** Building the generator (what level.dat decoding does) must not build the machine catalog; the first chunk does. */
    @GameTest(template = "qa_empty", batch = "plot_codec", timeoutTicks = 40)
    public static void campusGeneratorDoesNotBuildTheCatalogWhenCreated(GameTestHelper helper) {
        var biome = helper.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME)
                .getHolderOrThrow(net.minecraft.world.level.biome.Biomes.PLAINS);
        int before = com.raishxn.modernspace.campus.catalog.RuntimeCampusCatalog.requests();
        new PlotChunkGenerator(biome, PlotSettings.plannedCampus());
        new PlotChunkGenerator(biome, PlotSettings.factoryCampus());
        helper.assertTrue(com.raishxn.modernspace.campus.catalog.RuntimeCampusCatalog.requests() == before,
                "creating a campus generator asked for the machine catalog");
        helper.succeed();
    }

    /** Settings keep the recipe through NBT and the network buffer, and the editor rejects an unreadable one. */
    @GameTest(template = "qa_empty", batch = "plot_codec", timeoutTicks = 40)
    public static void plotPresetSurvivesSaveAndSync(GameTestHelper helper) {
        PersonalSpaceSettings settings = PersonalSpaceSettings.voidWorld();
        settings.setPlotPreset(PlotPresetShare.encode(PlotSettings.fileCampus("quartz_works")));
        PersonalSpaceSettings loaded = PersonalSpaceSettings.load(settings.save());
        helper.assertTrue(settings.equals(loaded), "NBT round trip changed the settings");
        var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        settings.write(buffer);
        helper.assertTrue(settings.equals(PersonalSpaceSettings.read(buffer)), "network round trip changed the settings");
        helper.assertTrue(loaded.getGroundLevel() == loaded.plotSettings().surfaceY() + 1, "ground level ignores the plot");
        PersonalSpaceSettings broken = PersonalSpaceSettings.voidWorld();
        broken.setPlotPreset("not a preset");
        helper.assertTrue(broken.plotSettings() == null, "an unreadable recipe was accepted");
        helper.succeed();
    }

    static ServerLevel create(GameTestHelper helper, PlotSettings plot) {
        var server = helper.getLevel().getServer();
        PersonalSpaceSettings settings = PersonalSpaceSettings.voidWorld();
        settings.setPlotPreset(PlotPresetShare.encode(plot));
        int id = PersonalSpaceWorlds.create(server, settings);
        ServerLevel level = PersonalSpaceWorlds.load(server, id);
        helper.assertTrue(level != null, "dimension " + id + " was not created");
        helper.assertTrue(level.getChunkSource().getGenerator() instanceof PlotChunkGenerator,
                "dimension " + id + " does not use the plot generator");
        helper.assertTrue(level.getMinBuildHeight() == -64 && level.getMaxBuildHeight() == 320,
                "plot dimension height is " + level.getMinBuildHeight() + ".." + level.getMaxBuildHeight());
        helper.assertTrue(PersonalSpaceDirectory.get(server).settings(id).isPlotWorld(), "directory lost the recipe");
        return level;
    }

    private static void assertTeleporter(GameTestHelper helper, ServerLevel level, PlotSettings plot) {
        BlockPos teleporter = new BlockPos(0, plot.surfaceY() + 1, 0);
        helper.assertTrue(level.getBlockState(teleporter).is(CampusDeviceBlocks.CENTRAL_TELEPORTER.get()),
                plot.campusKey() + ": no central teleporter at " + teleporter + ", found " + level.getBlockState(teleporter));
        helper.assertTrue(level.getBlockEntity(teleporter) != null, plot.campusKey() + ": teleporter has no block entity");
    }
}
