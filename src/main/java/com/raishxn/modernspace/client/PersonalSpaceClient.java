package com.raishxn.modernspace.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;

import com.raishxn.modernspace.ModernSpace;
import com.raishxn.modernspace.client.gui.PersonalSpaceEditScreen;
import com.raishxn.modernspace.common.PersonalSpaceClientState;
import com.raishxn.modernspace.common.PersonalSpacePortalEntity;
import com.raishxn.modernspace.common.PersonalSpacePortalRegistry;

/** Client registration of the PersonalSpace editor, portal book and sky effects. */
public final class PersonalSpaceClient {

    private PersonalSpaceClient() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(PersonalSpaceClient::registerRenderers);
        modBus.addListener(PersonalSpaceClient::registerEffects);
        MinecraftForge.EVENT_BUS.addListener(PersonalSpaceClient::loggingOut);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(PersonalSpacePortalRegistry.PORTAL_ENTITY.get(),
                PersonalSpacePortalRenderer::new);
    }

    private static void registerEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(ModernSpace.id("personal_space"), new PersonalSpaceEffects());
    }

    private static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        PersonalSpaceClientState.clear();
    }

    public static void openPortalGui(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockEntity(pos) instanceof PersonalSpacePortalEntity portal) {
            mc.setScreen(new PersonalSpaceEditScreen(portal));
        }
    }

    public static void closePortalGui(PersonalSpacePortalEntity portal) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof PersonalSpaceEditScreen screen && screen.tile == portal) mc.setScreen(null);
    }
}
