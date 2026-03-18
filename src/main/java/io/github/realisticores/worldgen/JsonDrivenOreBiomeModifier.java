package io.github.realisticores.worldgen;

import com.mojang.serialization.Codec;
import io.github.realisticores.RealisticOresMod;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class JsonDrivenOreBiomeModifier implements BiomeModifier {
    private static final JsonDrivenOreBiomeModifier INSTANCE = new JsonDrivenOreBiomeModifier();
    private static final DeferredRegister<Codec<? extends BiomeModifier>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, RealisticOresMod.MOD_ID);

    public static final RegistryObject<Codec<? extends BiomeModifier>> JSON_DRIVEN_ORE_BIOME_MODIFIER =
            SERIALIZERS.register("json_driven_ore_biome_modifier", () -> Codec.unit(INSTANCE));

    private JsonDrivenOreBiomeModifier() {
    }

    public static void register(IEventBus modBus) {
        SERIALIZERS.register(modBus);
    }

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase == Phase.REMOVE) {
            FeatureRemovalHandler.removeDisabledFeatures(builder.getGenerationSettings());
            return;
        }

        if (phase == Phase.ADD) {
            OreFeatureFactory.definitions().forEach(definition -> OreFeatureFactory.addFeatureToBiome(definition, biome, builder.getGenerationSettings()));
        }
    }

    @Override
    public Codec<? extends BiomeModifier> codec() {
        return JSON_DRIVEN_ORE_BIOME_MODIFIER.get();
    }
}
