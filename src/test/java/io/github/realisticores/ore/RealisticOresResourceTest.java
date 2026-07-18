package io.github.realisticores.ore;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

final class RealisticOresResourceTest {
    private static final Gson GSON = new Gson();
    private static final Path DATA_ROOT = Path.of("src/main/resources/data/realisticores");
    private static final int[] SURFACE_SAMPLE_UV = {8, 8, 10, 11};

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
                    Path modelPath = resources.resolve(
                            "assets/realisticores/models/block/" + crushed + "_" + variant + ".json");
                    assertTrue(Files.isRegularFile(modelPath), crushed);
                    assertSurfaceSampleModelUsesOpaqueUvs(modelPath);
                }
                JsonObject itemModel = read(
                        resources.resolve("assets/realisticores/models/item/" + crushed + ".json"),
                        JsonObject.class);
                String texture = itemModel.getAsJsonObject("textures").get("layer0").getAsString();
                assertTrue(texture.startsWith("realisticores:item/"), crushed + " texture " + texture);
                assertOpaqueUvPatch(resources.resolve(
                        "assets/realisticores/textures/item/" + texture.substring("realisticores:item/".length()) + ".png"));
            }
        }
        assertTrue(Files.isRegularFile(resources.resolve("assets/realisticores/blockstates/oil_seep.json")));
        assertTrue(Files.isRegularFile(resources.resolve("data/realisticores/loot_tables/blocks/oil_seep.json")));
    }

    private static void assertSurfaceSampleModelUsesOpaqueUvs(Path modelPath) {
        JsonObject model = read(modelPath, JsonObject.class);
        assertEquals("#all", model.getAsJsonObject("textures").get("particle").getAsString(), modelPath.toString());
        for (var element : model.getAsJsonArray("elements")) {
            for (var face : element.getAsJsonObject().getAsJsonObject("faces").entrySet()) {
                JsonArray uv = face.getValue().getAsJsonObject().getAsJsonArray("uv");
                assertEquals(4, uv.size(), modelPath + " " + face.getKey());
                for (int index = 0; index < SURFACE_SAMPLE_UV.length; index++) {
                    assertEquals(SURFACE_SAMPLE_UV[index], uv.get(index).getAsInt(), modelPath + " " + face.getKey());
                }
            }
        }
    }

    private static void assertOpaqueUvPatch(Path texturePath) throws IOException {
        BufferedImage texture = ImageIO.read(texturePath.toFile());
        assertTrue(texture != null, texturePath.toString());
        for (int y = SURFACE_SAMPLE_UV[1]; y < SURFACE_SAMPLE_UV[3]; y++) {
            for (int x = SURFACE_SAMPLE_UV[0]; x < SURFACE_SAMPLE_UV[2]; x++) {
                int alpha = texture.getRGB(x, y) >>> 24;
                assertEquals(255, alpha, texturePath + " alpha at " + x + "," + y);
            }
        }
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
