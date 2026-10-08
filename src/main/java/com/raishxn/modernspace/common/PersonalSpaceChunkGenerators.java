package com.raishxn.modernspace.common;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import com.mojang.serialization.Codec;
import com.raishxn.modernspace.ModernSpace;

public final class PersonalSpaceChunkGenerators {

    private static final DeferredRegister<Codec<? extends ChunkGenerator>> GENERATORS = DeferredRegister
            .create(Registries.CHUNK_GENERATOR, ModernSpace.MOD_ID);
    public static final RegistryObject<Codec<? extends ChunkGenerator>> CODEC = GENERATORS.register("personal_space",
            () -> PersonalSpaceChunkGenerator.CODEC);

    private PersonalSpaceChunkGenerators() {}

    public static void register(IEventBus bus) {
        GENERATORS.register(bus);
    }
}
