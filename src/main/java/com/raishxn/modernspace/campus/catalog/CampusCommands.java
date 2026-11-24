package com.raishxn.modernspace.campus.catalog;

import com.raishxn.modernspace.ModernSpace;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** /modernspace_campus dumpcatalog — exports the runtime machine catalog to config/modernspace/catalog-dump.json. */
@Mod.EventBusSubscriber(modid = ModernSpace.MOD_ID)
public final class CampusCommands {
    private CampusCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("modernspace_campus").requires(source -> source.hasPermission(2))
                .then(Commands.literal("dumpcatalog").executes(CampusCommands::dump)));
    }

    private static int dump(CommandContext<CommandSourceStack> context) {
        try {
            var path = RuntimeCampusCatalog.dump();
            var json = RuntimeCampusCatalog.json();
            context.getSource().sendSuccess(() -> Component.translatable("modernspace.command.dumpcatalog.ok",
                    json.getAsJsonArray("reservations").size(), json.getAsJsonArray("effectiveSharedIds").size(),
                    json.getAsJsonArray("skipped").size(), path.toAbsolutePath().toString()), true);
            return 1;
        } catch (Exception error) {
            ModernSpace.LOGGER.error("[campus] dumpcatalog failed", error);
            context.getSource().sendFailure(Component.translatable("modernspace.command.dumpcatalog.fail", error.toString()));
            return 0;
        }
    }
}
