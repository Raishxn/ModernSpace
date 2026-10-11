package com.raishxn.modernspace.mixin;

import com.raishxn.modernspace.common.GtnaMigration;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Imports a GTNA PersonalSpace world as soon as its folder is opened (session lock held), before level.dat is read
 * by either the integrated or the dedicated server.
 */
@Mixin(LevelStorageSource.LevelStorageAccess.class)
public abstract class LevelStorageAccessMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void modernspace$importGtnaPersonalSpace(CallbackInfo ci) {
        GtnaMigration.migrateWorld(((LevelStorageSource.LevelStorageAccess) (Object) this)
                .getLevelPath(LevelResource.ROOT));
    }
}
