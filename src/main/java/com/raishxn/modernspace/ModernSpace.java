package com.raishxn.modernspace;

import com.mojang.logging.LogUtils;
import com.raishxn.modernspace.common.PersonalSpaceChunkGenerators;
import com.raishxn.modernspace.common.PersonalSpaceConfig;
import com.raishxn.modernspace.common.PersonalSpaceEvents;
import com.raishxn.modernspace.common.PersonalSpacePortalRegistry;
import com.raishxn.modernspace.network.ModernSpaceNetwork;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

/**
 * ModernSpace: GTNH PersonalSpace (GTNewHorizons/PersonalSpace, LGPL-3.0) for Forge 1.20.1, ported from the
 * GregTech Nexus Addon. Personal dimensions with configurable layers, sky and weather, entered through a portal.
 */
@Mod(ModernSpace.MOD_ID)
public final class ModernSpace {

    public static final String MOD_ID = "modernspace";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public ModernSpace(FMLJavaModLoadingContext context) {
        var bus = context.getModEventBus();
        PersonalSpaceChunkGenerators.register(bus);
        PersonalSpacePortalRegistry.register(bus);
        ModernSpaceNetwork.register();
        PersonalSpaceConfig.load();
        PersonalSpaceEvents.register();
        bus.addListener(this::tabs);
        if (FMLEnvironment.dist.isClient()) com.raishxn.modernspace.client.PersonalSpaceClient.init(bus);
    }

    private void tabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) event.accept(PersonalSpacePortalRegistry.PORTAL_ITEM);
    }
}
