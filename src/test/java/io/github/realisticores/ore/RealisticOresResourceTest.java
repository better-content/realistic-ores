package io.github.realisticores.ore;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

final class RealisticOresResourceTest {
    private static final Gson GSON = new Gson();
    private static final Path DATA_ROOT = Path.of("src/main/resources/data/realisticores");

    @Test
    void packagedOreDefinitionsAndGenerationEntriesAreConsistent() throws IOException {
        Set<OreVariant> oreVariants;
        try (var paths = Files.list(DATA_ROOT.resolve("realistic_ores"))) {
            oreVariants = paths
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .map(path -> read(path, OreDefinition.class))
                    .peek(OreDefinition::validate)
                    .flatMap(definition -> definition.variants().stream()
                            .map(variant -> new OreVariant(definition.id(), variant.host())))
                    .collect(Collectors.toUnmodifiableSet());
        }

        assertFalse(oreVariants.isEmpty(), "expected ore definition resources");
        try (var paths = Files.list(DATA_ROOT.resolve("realistic_ore_generation"))) {
            var generationPaths = paths.filter(file -> file.getFileName().toString().endsWith(".json")).toList();
            assertFalse(generationPaths.isEmpty(), "expected realistic ore generation resources");
            for (Path path : generationPaths) {
                GenerationDefinition definition = read(path, GenerationDefinition.class);
                assertTrue(oreVariants.contains(new OreVariant(definition.oreId, definition.variant)),
                        "generation entry references unknown ore variant in " + path);
            }
        }
    }

    @Test
    void disabledPlacedFeatureResourcesValidate() throws IOException {
        try (var paths = Files.list(DATA_ROOT.resolve("disabled_placed_features"))) {
            var resources = paths.filter(path -> path.getFileName().toString().endsWith(".json")).toList();
            assertFalse(resources.isEmpty(), "expected disabled placed feature resources");
            resources.stream()
                    .map(path -> read(path, DisabledFeaturesDefinition.class))
                    .forEach(DisabledFeaturesDefinition::validate);
        }
    }

    @Test
    void everyCrushedOreHasGroundSampleResources() throws IOException {
        Path resources = Path.of("src/main/resources");
        Path definitions = resources.resolve("data/realisticores/realistic_ores");
        try (var paths = Files.list(definitions)) {
            for (Path path : paths.filter(file -> file.getFileName().toString().endsWith(".json")).toList()) {
                OreDefinition definition = read(path, OreDefinition.class);
                String crushed = definition.crushedItemId();
                assertTrue(Files.isRegularFile(resources.resolve("assets/realisticores/blockstates/" + crushed + ".json")), crushed);
                assertTrue(Files.isRegularFile(resources.resolve("data/realisticores/loot_tables/blocks/" + crushed + ".json")), crushed);
                for (int variant = 0; variant < 5; variant++) {
                    assertTrue(Files.isRegularFile(resources.resolve(
                            "assets/realisticores/models/block/" + crushed + "_" + variant + ".json")), crushed);
                }
            }
        }
        assertTrue(Files.isRegularFile(resources.resolve("assets/realisticores/blockstates/oil_seep.json")));
        assertTrue(Files.isRegularFile(resources.resolve("data/realisticores/loot_tables/blocks/oil_seep.json")));
    }

    private record OreVariant(String oreId, String variant) {
    }

    private static <T> T read(Path path, Class<T> type) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, type);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read " + path, exception);
        }
    }

    private static final class GenerationDefinition {
        @com.google.gson.annotations.SerializedName("ore_id")
        private String oreId;
        private String variant;
    }
}
