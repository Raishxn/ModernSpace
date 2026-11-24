package com.raishxn.modernspace.campus.item;

import com.raishxn.modernspace.campus.info.PlotWorldInfoNetwork;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * EN: opens the plot/campus panel of the dimension the player stands in (info, campus map, share code).
 * PT: abre o painel de lote/campus da dimensão onde o jogador está (informações, mapa do campus, código).
 * ZH: 打开玩家所在维度的地块/园区面板（信息、园区地图、分享码）。
 */
public final class PlotWorldControllerItem extends Item {
    public PlotWorldControllerItem(Properties properties) { super(properties.stacksTo(1)); }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.success(stack);
        if (!player.getCooldowns().isOnCooldown(this)) {
            player.getCooldowns().addCooldown(this, 10);
            PlotWorldInfoNetwork.send(serverPlayer);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.modernspace.plot_world_controller.modernspace_usage")
                .withStyle(ChatFormatting.AQUA));
    }
}
