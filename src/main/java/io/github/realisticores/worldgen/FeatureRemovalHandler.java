package io.github.realisticores.worldgen;

import io.github.realisticores.ore.DisabledFeaturesLoader;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.common.world.BiomeGenerationSettingsBuilder;

public final class FeatureRemovalHandler {
    private static final Set<ResourceLocation> DISABLED_FEATURES = DisabledFeaturesLoader.loadAll().stream()
            .map(FeatureRemovalHandler::parseResourceLocation)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());

    private FeatureRemovalHandler() {
    }

    public static void removeDisabledFeatures(BiomeGenerationSettingsBuilder generationSettings) {
        for (Decoration decoration : Decoration.values()) {
            generationSettings.getFeatures(decoration).removeIf(FeatureRemovalHandler::isDisabled);
        }
    }

    private static boolean isDisabled(net.minecraft.core.Holder<PlacedFeature> featureHolder) {
        return featureHolder.unwrapKey()
                .map(key -> DISABLED_FEATURES.contains(key.location()))
                .orElse(false);
    }

    private static ResourceLocation parseResourceLocation(String value) {
        ResourceLocation location = ResourceLocation.tryParse(value);
        if (location == null) {
            throw new IllegalArgumentException("Invalid placed feature id " + value);
        }
        return location;
    }
}
