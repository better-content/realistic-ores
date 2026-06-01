package io.github.realisticores.registry;

import io.github.realisticores.RealisticOresMod;
import io.github.realisticores.worldgen.LavaExposedOreFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(ForgeRegistries.FEATURES, RealisticOresMod.MOD_ID);

    public static final RegistryObject<Feature<OreConfiguration>> LAVA_EXPOSED_ORE =
            FEATURES.register("lava_exposed_ore", () -> new LavaExposedOreFeature(OreConfiguration.CODEC));

    private ModFeatures() {
    }

    public static void register(IEventBus modBus) {
        FEATURES.register(modBus);
    }
}
