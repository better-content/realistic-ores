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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Map;
import javax.imageio.ImageIO;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

final class RealisticOresResourceTest {
    private static final Gson GSON = new Gson();
    private static final Path DATA_ROOT = Path.of("src/main/resources/data/realisticores");
    private static final Path RESOURCE_ROOT = Path.of("src/main/resources");
    private static final Path ASSET_ROOT = RESOURCE_ROOT.resolve("assets/realisticores");
    private static final Set<Integer> STONE_COLORS = rgbSet("686868", "747474", "7f7f7f", "8f8f8f");
    private static final Set<Integer> DEEPSLATE_SIDE_COLORS = rgbSet("2f2f37", "3d3d43", "515151", "646464", "797979");
    private static final Set<Integer> DEEPSLATE_END_COLORS = rgbSet("3d3d43", "4b4b50", "5a5a5a", "646464", "747474");
    private static final Set<String> FACES = Set.of("north", "east", "south", "west", "up", "down");

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
    void everyOreBlockHasThreeUnrotatedSidedModelsAndValidFinalTextures() throws IOException {
        JsonObject palettes = read(Path.of("src/test/resources/ore_texture_palettes.json"), JsonObject.class);
        JsonObject canonicalHashes = read(Path.of("src/test/resources/canonical_ore_texture_hashes.json"), JsonObject.class);

        try (var paths = Files.list(DATA_ROOT.resolve("realistic_ores"))) {
            for (Path path : paths.filter(file -> file.getFileName().toString().endsWith(".json")).toList()) {
                OreDefinition definition = read(path, OreDefinition.class);
                definition.validate();
                Set<Integer> palette = new HashSet<>();
                palettes.getAsJsonArray(definition.id()).forEach(color -> palette.add(parseRgb(color.getAsString())));

                for (OreDefinition.VariantDefinition oreVariant : definition.variants()) {
                    assertEquals(OreDefinition.TextureMode.CUBE_SIDED, oreVariant.textureMode(), path.toString());
                    String blockId = oreVariant.blockId();
                    String textureBlockId = blockId;
                    assertCanonicalDefinitionTextures(oreVariant, textureBlockId);
                    assertWeightedBlockstate(blockId);
                    assertItemUsesCanonicalModel(blockId);
                    assertFalse(Files.exists(ASSET_ROOT.resolve("models/block/" + blockId + ".json")), blockId);

                    Set<String> hashes = new HashSet<>();
                    for (int variant = 0; variant < 3; variant++) {
                        Path modelPath = ASSET_ROOT.resolve("models/block/" + blockId + "_" + variant + ".json");
                        JsonObject model = read(modelPath, JsonObject.class);
                        assertEquals("minecraft:block/cube", model.get("parent").getAsString(), modelPath.toString());
                        JsonObject textures = model.getAsJsonObject("textures");
                        assertEquals(textureRef(textureBlockId, variant, "south"), textures.get("particle").getAsString(), modelPath.toString());
                        for (String face : FACES) {
                            String expectedTexture = textureRef(textureBlockId, variant, face);
                            assertEquals(expectedTexture, textures.get(face).getAsString(), modelPath + " " + face);
                            Path texturePath = ASSET_ROOT.resolve("textures/block/" + textureBlockId + "_" + variant + "_" + face + ".png");
                            assertFinalTexture(texturePath, palette, hostColors(oreVariant.host(), face),
                                    isCanonicalAnchor(definition.id(), oreVariant.host(), variant, face));
                            assertTrue(hashes.add(sha256(texturePath)), "duplicate face texture: " + texturePath);
                        }
                    }
                    assertEquals(18, hashes.size(), blockId);
                    assertCanonicalHashes(definition.id(), oreVariant.host(), canonicalHashes);
                }
            }
        }
    }

    private static void assertCanonicalDefinitionTextures(
            OreDefinition.VariantDefinition variant,
            String textureBlockId
    ) {
        Map<String, String> actual = Map.of(
                "north", variant.textures().north(),
                "east", variant.textures().east(),
                "south", variant.textures().south(),
                "west", variant.textures().west(),
                "up", variant.textures().up(),
                "down", variant.textures().down());
        for (String face : FACES) {
            assertEquals(textureRef(textureBlockId, 0, face), actual.get(face), variant.blockId() + " " + face);
        }
    }

    private static void assertWeightedBlockstate(String blockId) {
        Path statePath = ASSET_ROOT.resolve("blockstates/" + blockId + ".json");
        JsonArray variants = read(statePath, JsonObject.class).getAsJsonObject("variants").getAsJsonArray("");
        assertEquals(3, variants.size(), statePath.toString());
        for (int index = 0; index < variants.size(); index++) {
            JsonObject entry = variants.get(index).getAsJsonObject();
            assertEquals(Set.of("model"), entry.keySet(), "rotations, mirrors, and explicit weights are forbidden: " + statePath);
            assertEquals("realisticores:block/" + blockId + "_" + index, entry.get("model").getAsString(), statePath.toString());
        }
    }

    private static void assertItemUsesCanonicalModel(String blockId) {
        Path itemPath = ASSET_ROOT.resolve("models/item/" + blockId + ".json");
        assertEquals("realisticores:block/" + blockId + "_0",
                read(itemPath, JsonObject.class).get("parent").getAsString(), itemPath.toString());
    }

    private static void assertFinalTexture(
            Path texturePath,
            Set<Integer> palette,
            Set<Integer> hostColors,
            boolean canonicalAnchor
    ) throws IOException {
        BufferedImage image = ImageIO.read(texturePath.toFile());
        assertTrue(image != null, texturePath.toString());
        assertEquals(16, image.getWidth(), texturePath.toString());
        assertEquals(16, image.getHeight(), texturePath.toString());
        int mineralPixels = 0;
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int argb = image.getRGB(x, y);
                assertEquals(255, argb >>> 24, texturePath + " alpha at " + x + "," + y);
                int rgb = argb & 0xffffff;
                if (!hostColors.contains(rgb)) {
                    mineralPixels++;
                    if (!canonicalAnchor) {
                        assertTrue(palette.contains(rgb), texturePath + " contains off-palette color #" + String.format("%06x", rgb));
                    }
                }
            }
        }
        if (canonicalAnchor) {
            assertTrue(mineralPixels > 0, texturePath + " has no mineral pixels");
        } else {
            assertTrue(mineralPixels >= 20 && mineralPixels <= 33,
                    texturePath + " has " + mineralPixels + " mineral pixels");
        }
    }

    private static void assertCanonicalHashes(String family, String host, JsonObject manifest) {
        JsonObject hashes = manifest.getAsJsonObject(family);
        if (host.equals("stone")) {
            assertEquals(hashes.get("stone_0_south").getAsString(),
                    sha256(ASSET_ROOT.resolve("textures/block/" + blockId(family) + "_0_south.png")), family);
        } else {
            String blockId = "deepslate_" + blockId(family);
            assertEquals(hashes.get("deepslate_0_south").getAsString(),
                    sha256(ASSET_ROOT.resolve("textures/block/" + blockId + "_0_south.png")), family);
            assertEquals(hashes.get("deepslate_0_up").getAsString(),
                    sha256(ASSET_ROOT.resolve("textures/block/" + blockId + "_0_up.png")), family);
        }
    }

    private static boolean isCanonicalAnchor(String family, String host, int variant, String face) {
        return !family.equals("osmiridium_lava_sulfide")
                && variant == 0
                && (face.equals("south") || (host.equals("deepslate") && face.equals("up")));
    }

    private static Set<Integer> hostColors(String host, String face) {
        if (host.equals("stone")) {
            return STONE_COLORS;
        }
        return face.equals("up") || face.equals("down") ? DEEPSLATE_END_COLORS : DEEPSLATE_SIDE_COLORS;
    }

    private static String textureRef(String blockId, int variant, String face) {
        return "realisticores:block/" + blockId + "_" + variant + "_" + face;
    }

    private static String blockId(String family) {
        return switch (family) {
            case "copper_sulfide" -> "copper_sulfide_ore";
            case "nickel_sulfide" -> "nickel_sulfide_ore";
            case "osmiridium_lava_sulfide" -> "osmiridium_lava_sulfide_ore";
            case "sulfur_bearing_pyrite" -> "sulfur_bearing_pyrite_ore";
            case "thorium" -> "thorium_ore";
            case "tin" -> "tin_ore";
            case "titanium_iron_oxide" -> "titanium_iron_oxide_ore";
            case "uranium" -> "uranium_ore";
            case "zinc" -> "zinc_ore";
            default -> family;
        };
    }

    private static Set<Integer> rgbSet(String... colors) {
        return java.util.Arrays.stream(colors)
                .map(color -> Integer.parseUnsignedInt(color, 16))
                .collect(Collectors.toUnmodifiableSet());
    }

    private static int parseRgb(String value) {
        return Integer.parseUnsignedInt(value.substring(1), 16);
    }

    private static String sha256(Path path) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Failed to hash " + path, exception);
        }
    }

    @Test
    void crushedItemsAndSurfaceSamplesHaveSeparateResources() throws IOException {
        Path resources = Path.of("src/main/resources");
        Path definitions = resources.resolve("data/realisticores/realistic_ores");
        try (var paths = Files.list(definitions)) {
            for (Path path : paths.filter(file -> file.getFileName().toString().endsWith(".json")).toList()) {
                OreDefinition definition = read(path, OreDefinition.class);
                String crushed = definition.crushedItemId();
                String sample = definition.surfaceSampleBlockId();
                assertFalse(Files.exists(resources.resolve("assets/realisticores/blockstates/" + crushed + ".json")), crushed);
                assertFalse(Files.exists(resources.resolve("data/realisticores/loot_tables/blocks/" + crushed + ".json")), crushed);
                assertTrue(Files.isRegularFile(resources.resolve("assets/realisticores/blockstates/" + sample + ".json")), sample);
                assertTrue(Files.isRegularFile(resources.resolve("data/realisticores/loot_tables/blocks/" + sample + ".json")), sample);
                for (int variant = 0; variant < 5; variant++) {
                    Path modelPath = resources.resolve(
                            "assets/realisticores/models/block/" + sample + "_" + variant + ".json");
                    assertTrue(Files.isRegularFile(modelPath), sample);
                    assertSurfaceSampleModelUsesOpaqueOreTexture(resources, modelPath);
                }
                JsonObject itemModel = read(
                        resources.resolve("assets/realisticores/models/item/" + crushed + ".json"),
                        JsonObject.class);
                String texture = itemModel.getAsJsonObject("textures").get("layer0").getAsString();
                assertTrue(texture.startsWith("realisticores:item/"), crushed + " texture " + texture);
                assertTrue(Files.isRegularFile(resources.resolve(
                        "assets/realisticores/textures/item/" + texture.substring("realisticores:item/".length()) + ".png")));
                JsonObject sampleItemModel = read(
                        resources.resolve("assets/realisticores/models/item/" + sample + ".json"),
                        JsonObject.class);
                assertEquals("realisticores:block/" + sample + "_2",
                        sampleItemModel.get("parent").getAsString(), sample);
            }
        }
        assertTrue(Files.isRegularFile(resources.resolve("assets/realisticores/blockstates/oil_seep.json")));
        assertTrue(Files.isRegularFile(resources.resolve("data/realisticores/loot_tables/blocks/oil_seep.json")));
    }

    private static void assertSurfaceSampleModelUsesOpaqueOreTexture(Path resources, Path modelPath) throws IOException {
        JsonObject model = read(modelPath, JsonObject.class);
        assertEquals("#all", model.getAsJsonObject("textures").get("particle").getAsString(), modelPath.toString());
        String texture = model.getAsJsonObject("textures").get("all").getAsString();
        assertTrue(texture.startsWith("realisticores:block/"), modelPath + " texture " + texture);
        Path texturePath = resources.resolve(
                "assets/realisticores/textures/block/" + texture.substring("realisticores:block/".length()) + ".png");
        BufferedImage image = ImageIO.read(texturePath.toFile());
        assertTrue(image != null, texturePath.toString());
        assertEquals(16, image.getWidth(), texturePath.toString());
        assertEquals(16, image.getHeight(), texturePath.toString());
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                assertEquals(255, image.getRGB(x, y) >>> 24, texturePath + " alpha at " + x + "," + y);
            }
        }
        for (var element : model.getAsJsonArray("elements")) {
            for (var face : element.getAsJsonObject().getAsJsonObject("faces").entrySet()) {
                assertEquals("#all", face.getValue().getAsJsonObject().get("texture").getAsString(),
                        modelPath + " " + face.getKey());
                JsonArray uv = face.getValue().getAsJsonObject().getAsJsonArray("uv");
                assertEquals(4, uv.size(), modelPath + " " + face.getKey());
                assertTrue(uv.get(0).getAsInt() >= 0 && uv.get(1).getAsInt() >= 0,
                        modelPath + " " + face.getKey());
                assertTrue(uv.get(2).getAsInt() <= 16 && uv.get(3).getAsInt() <= 16,
                        modelPath + " " + face.getKey());
                assertFalse(uv.get(0).getAsInt() == 0 && uv.get(1).getAsInt() == 0
                                && uv.get(2).getAsInt() == 16 && uv.get(3).getAsInt() == 16,
                        "surface chips must crop the ore texture instead of squashing a whole block face: " + modelPath);
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
