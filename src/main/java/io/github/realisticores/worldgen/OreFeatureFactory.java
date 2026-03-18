package io.github.realisticores.worldgen;

import io.github.realisticores.RealisticOresMod;
import io.github.realisticores.ore.WorldgenDefinition;
import io.github.realisticores.ore.WorldgenDefinitionLoader;
import io.github.realisticores.registry.ModBlocks;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration.TargetBlockState;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraftforge.common.world.BiomeGenerationSettingsBuilder;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class OreFeatureFactory {
    public static final DeferredRegister<ConfiguredFeature<?, ?>> CONFIGURED_FEATURES = DeferredRegister.create(Registries.CONFIGURED_FEATURE, RealisticOresMod.MOD_ID);
    public static final DeferredRegister<PlacedFeature> PLACED_FEATURES = DeferredRegister.create(Registries.PLACED_FEATURE, RealisticOresMod.MOD_ID);
    private static final List<WorldgenDefinition> WORLDGEN_DEFINITIONS = WorldgenDefinitionLoader.loadAll();
    private static boolean initialized;

    private OreFeatureFactory() {
    }

    public static void register(IEventBus modBus) {
        if (!initialized) {
            initialized = true;
            for (WorldgenDefinition definition : WORLDGEN_DEFINITIONS) {
                registerDefinition(definition);
            }
        }
        CONFIGURED_FEATURES.register(modBus);
        PLACED_FEATURES.register(modBus);
    }

    public static List<WorldgenDefinition> definitions() {
        return WORLDGEN_DEFINITIONS;
    }

    public static void addFeatureToBiome(WorldgenDefinition definition, Holder<Biome> biome, BiomeGenerationSettingsBuilder generationSettings) {
        if (!definition.enabled() || !matchesBiomeFilter(definition, biome)) {
            return;
        }

        generationSettings.addFeature(resolveStep(definition.generationStep()), placedFeature(definition).getHolder().orElseThrow());
    }

    private static void registerDefinition(WorldgenDefinition definition) {
        String registryId = definition.registryId();
        RegistryObject<ConfiguredFeature<?, ?>> configured = CONFIGURED_FEATURES.register(registryId, () -> new ConfiguredFeature<>(Feature.ORE, createOreConfiguration(definition)));
        PLACED_FEATURES.register(registryId, () -> new PlacedFeature(configured.getHolder().orElseThrow(), placementModifiers(definition)));
    }

    private static OreConfiguration createOreConfiguration(WorldgenDefinition definition) {
        TagKey<net.minecraft.world.level.block.Block> targetTag = TagKey.create(Registries.BLOCK, parseResourceLocation(definition.targetTag()));
        List<TargetBlockState> targets = List.of(OreConfiguration.target(new net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest(targetTag), ModBlocks.getBlock(definition.oreId(), definition.variant()).get().defaultBlockState()));
        return new OreConfiguration(targets, definition.veinSize(), definition.discardChanceOnAirExposure());
    }

    private static List<PlacementModifier> placementModifiers(WorldgenDefinition definition) {
        List<PlacementModifier> modifiers = new ArrayList<>();
        modifiers.add(CountPlacement.of(ConstantInt.of(definition.countPerChunk())));
        modifiers.add(InSquarePlacement.spread());
        modifiers.add(switch (definition.distribution()) {
            case UNIFORM -> HeightRangePlacement.uniform(VerticalAnchor.absolute(definition.minY()), VerticalAnchor.absolute(definition.maxY()));
            case TRIANGLE -> HeightRangePlacement.triangle(VerticalAnchor.absolute(definition.minY()), VerticalAnchor.absolute(definition.maxY()));
        });
        modifiers.add(BiomeFilter.biome());
        return modifiers;
    }

    private static RegistryObject<PlacedFeature> placedFeature(WorldgenDefinition definition) {
        return PLACED_FEATURE(definition.registryId());
    }

    private static RegistryObject<PlacedFeature> PLACED_FEATURE(String registryId) {
        return PLACED_FEATURES.getEntries().stream()
                .filter(entry -> entry.getId().getPath().equals(registryId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown placed feature " + registryId));
    }

    private static boolean matchesBiomeFilter(WorldgenDefinition definition, Holder<Biome> biome) {
        String biomeFilter = definition.biomeFilter();
        if (biomeFilter.startsWith("#")) {
            return biome.is(TagKey.create(Registries.BIOME, parseResourceLocation(biomeFilter.substring(1))));
        }
        return biome.unwrapKey()
                .map(key -> key.location().equals(parseResourceLocation(biomeFilter)))
                .orElse(false);
    }

    private static Decoration resolveStep(String generationStep) {
        return Decoration.valueOf(generationStep.toUpperCase(Locale.ROOT));
    }

    private static ResourceLocation parseResourceLocation(String value) {
        ResourceLocation location = ResourceLocation.tryParse(value);
        if (location == null) {
            throw new IllegalArgumentException("Invalid resource location " + value);
        }
        return location;
    }
}
