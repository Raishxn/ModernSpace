package com.raishxn.modernspace.gametest;

import com.raishxn.modernspace.ModernSpace;
import com.raishxn.modernspace.campus.device.CampusDeviceBlocks;
import com.raishxn.modernspace.campus.device.CentralTeleporterBlockEntity;
import com.raishxn.modernspace.campus.item.PortableCentralTeleporterItem;
import com.raishxn.modernspace.campus.plot.PlotSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * EN: the central teleporter keeps its campus binding in the saved chunk and the portable one keeps it in the item NBT.
 * PT: o teleportador central guarda o vínculo com o campus no chunk salvo e o portátil guarda no NBT do item.
 * ZH: 中央传送器的园区绑定保存在区块数据中，便携版则保存在物品 NBT 中。
 */
@GameTestHolder(ModernSpace.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CampusDeviceGameTests {
    private CampusDeviceGameTests() {}

    @GameTest(template = "qa_empty", batch = "plot_woodland", timeoutTicks = 400)
    public static void centralTeleporterBindingSurvivesChunkSaveAndReload(GameTestHelper helper) {
        PlotSettings plot = PlotSettings.plannedCampus();
        var level = CampusGameTests.create(helper, plot);
        BlockPos pos = new BlockPos(0, plot.surfaceY() + 1, 0);
        ResourceLocation dimension = level.dimension().location();
        helper.assertTrue(level.getBlockEntity(pos) instanceof CentralTeleporterBlockEntity terminal
                && dimension.equals(terminal.boundDimension()), "a freshly generated teleporter is not bound to its campus");

        LevelChunk chunk = level.getChunkAt(pos);
        CompoundTag saved = ChunkSerializer.write(level, chunk);
        CompoundTag entity = null;
        ListTag entities = saved.getList("block_entities", Tag.TAG_COMPOUND);
        for (int i = 0; i < entities.size(); i++) {
            CompoundTag candidate = entities.getCompound(i);
            if (candidate.getInt("x") == pos.getX() && candidate.getInt("y") == pos.getY() && candidate.getInt("z") == pos.getZ()) entity = candidate;
        }
        helper.assertTrue(entity != null, "the saved chunk has no block entity at " + pos);
        helper.assertTrue(dimension.toString().equals(entity.getString("BoundDimension")),
                "the saved chunk lost the binding: " + entity);

        var reloaded = ChunkSerializer.read(level, level.getPoiManager(), chunk.getPos(), saved);
        helper.assertTrue(reloaded instanceof ImposterProtoChunk, "the saved chunk did not come back as a full chunk");
        LevelChunk wrapped = ((ImposterProtoChunk) reloaded).getWrapped();
        wrapped.setBlockEntityNbt(entity.copy());
        BlockEntity again = wrapped.getBlockEntity(pos);
        helper.assertTrue(again instanceof CentralTeleporterBlockEntity, "the reloaded chunk has no teleporter entity: " + again);
        helper.assertTrue(dimension.equals(((CentralTeleporterBlockEntity) again).boundDimension()),
                "the reloaded teleporter is no longer bound: " + ((CentralTeleporterBlockEntity) again).boundDimension());

        CompoundTag onlyTag = entity.copy();
        BlockEntity direct = BlockEntity.loadStatic(pos, level.getBlockState(pos), onlyTag);
        helper.assertTrue(direct instanceof CentralTeleporterBlockEntity terminal && dimension.equals(terminal.boundDimension()),
                "BlockEntity.loadStatic dropped the binding");
        helper.succeed();
    }

    /** Outside a campus dimension a terminal stays unbound: only a real campus centre issues a binding. */
    @GameTest(template = "qa_empty", batch = "plot_grid", timeoutTicks = 40)
    public static void centralTeleporterOutsideCampusStaysUnbound(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlock(pos, CampusDeviceBlocks.CENTRAL_TELEPORTER.get().defaultBlockState(), 3);
        helper.assertTrue(level.getBlockEntity(pos) instanceof CentralTeleporterBlockEntity terminal && terminal.boundDimension() == null,
                "a terminal outside a campus became bound");
        helper.succeed();
    }

    @GameTest(template = "qa_empty", batch = "plot_codec", timeoutTicks = 40)
    public static void portableTeleporterKeepsItsBindingInItsNbt(GameTestHelper helper) {
        var binding = new PortableCentralTeleporterItem.Binding(new ResourceLocation(ModernSpace.MOD_ID, "personal_space/pdim_7"),
                new BlockPos(0, -52, 0), "campus-plan-v1:CAMPUS_WOODLAND:abc");
        ItemStack stack = new ItemStack(CampusDeviceBlocks.PORTABLE_CENTRAL_TELEPORTER.get());
        helper.assertTrue(PortableCentralTeleporterItem.binding(stack) == null, "a new portable teleporter is already bound");
        PortableCentralTeleporterItem.writeBinding(stack, binding);

        ItemStack reloaded = ItemStack.of(stack.save(new CompoundTag()));
        helper.assertTrue(binding.equals(PortableCentralTeleporterItem.binding(reloaded)), "the binding did not survive an item save");
        helper.assertTrue(binding.equals(PortableCentralTeleporterItem.binding(stack.copy())), "the binding did not survive a copy");
        helper.succeed();
    }
}
