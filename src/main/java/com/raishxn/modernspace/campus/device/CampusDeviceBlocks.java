package com.raishxn.modernspace.campus.device;

import com.raishxn.modernspace.ModernSpace;
import com.raishxn.modernspace.campus.item.PlotWorldControllerItem;
import com.raishxn.modernspace.campus.item.PortableCentralTeleporterItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Campus devices and items (plain DeferredRegister; the original used Registrate). */
public final class CampusDeviceBlocks {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ModernSpace.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ModernSpace.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ModernSpace.MOD_ID);

    public static final RegistryObject<VirtualMarkerBlock> VIRTUAL_MARKER = BLOCKS.register("virtual_marker",
            () -> new VirtualMarkerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.0F)
                    .sound(SoundType.GLASS).noCollission().noOcclusion().replaceable()));
    public static final RegistryObject<CentralTeleporterBlock> CENTRAL_TELEPORTER = BLOCKS.register("central_teleporter",
            () -> new CentralTeleporterBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(1.5F, 6.0F)
                    .sound(SoundType.METAL).noOcclusion()));
    public static final RegistryObject<Item> VIRTUAL_MARKER_ITEM = ITEMS.register("virtual_marker",
            () -> new BlockItem(VIRTUAL_MARKER.get(), new Item.Properties()));
    public static final RegistryObject<Item> CENTRAL_TELEPORTER_ITEM = ITEMS.register("central_teleporter",
            () -> new BlockItem(CENTRAL_TELEPORTER.get(), new Item.Properties()));
    public static final RegistryObject<Item> PLOT_WORLD_CONTROLLER = ITEMS.register("plot_world_controller",
            () -> new PlotWorldControllerItem(new Item.Properties()));
    public static final RegistryObject<Item> PORTABLE_CENTRAL_TELEPORTER = ITEMS.register("portable_central_teleporter",
            () -> new PortableCentralTeleporterItem(new Item.Properties()));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<VirtualMarkerBlockEntity>> VIRTUAL_MARKER_BLOCK_ENTITY = BLOCK_ENTITIES.register("virtual_marker",
            () -> BlockEntityType.Builder.<VirtualMarkerBlockEntity>of((p, s) -> new VirtualMarkerBlockEntity(CampusDeviceBlocks.VIRTUAL_MARKER_BLOCK_ENTITY.get(), p, s), VIRTUAL_MARKER.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<CentralTeleporterBlockEntity>> CENTRAL_TELEPORTER_BLOCK_ENTITY = BLOCK_ENTITIES.register("central_teleporter",
            () -> BlockEntityType.Builder.<CentralTeleporterBlockEntity>of((p, s) -> new CentralTeleporterBlockEntity(CampusDeviceBlocks.CENTRAL_TELEPORTER_BLOCK_ENTITY.get(), p, s), CENTRAL_TELEPORTER.get()).build(null));
    private CampusDeviceBlocks() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); BLOCK_ENTITIES.register(bus);
    }
}
