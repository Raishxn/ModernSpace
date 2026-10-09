package com.raishxn.modernspace.common;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.raishxn.modernspace.ModernSpace;

public final class PersonalSpacePortalRegistry {

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS,
            ModernSpace.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS,
            ModernSpace.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB,
            ModernSpace.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister
            .create(ForgeRegistries.BLOCK_ENTITY_TYPES, ModernSpace.MOD_ID);

    public static final RegistryObject<PersonalSpacePortalBlock> PORTAL = BLOCKS.register("personal_space_portal",
            PersonalSpacePortalBlock::new);
    public static final RegistryObject<Item> PORTAL_ITEM = ITEMS.register("personal_space_portal",
            () -> new PersonalSpacePortalItem(PORTAL.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<BlockEntityType<PersonalSpacePortalEntity>> PORTAL_ENTITY = BLOCK_ENTITIES
            .register("personal_space_portal", () -> BlockEntityType.Builder
                    .of(PersonalSpacePortalEntity::new, PORTAL.get()).build(null));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.modernspace"))
            .icon(() -> new ItemStack(PORTAL_ITEM.get()))
            .displayItems((parameters, output) -> output.accept(PORTAL_ITEM.get()))
            .build());

    private PersonalSpacePortalRegistry() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        TABS.register(bus);
    }
}
