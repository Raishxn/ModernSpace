package com.raishxn.modernspace.mixin.compat;

import com.raishxn.modernspace.common.PersonalSpaceConfig;
import com.raishxn.modernspace.common.PersonalSpaceWorlds;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * GregTech CEu compatibility: {@code GTUtil.canSeeSunClearly} drives solar panel covers and solar boilers. In a
 * personal space (fixed time and weather chosen by the owner) it follows the {@code gtceuSolarPanels} option: on, the
 * sun counts whenever the sky is visible; off, solar never works there. Applied only when GregTech is installed.
 */
@Pseudo
@Mixin(targets = "com.gregtechceu.gtceu.utils.GTUtil", remap = false)
public abstract class GTUtilSolarMixin {

    @Inject(method = "canSeeSunClearly(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z",
            at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void modernspace$personalSpaceSun(Level level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (level == null || PersonalSpaceWorlds.id(level.dimension()) <= 0) return;
        cir.setReturnValue(PersonalSpaceConfig.gtceuSolarPanels() && level.canSeeSky(pos.above()));
    }
}
