package com.bettercontent.realisticores.ore;

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
    private static final Path DATA_ROOT = Path.of("src/main/resources/data/realistic_ores");
    private static final Path RESOURCE_ROOT = Path.of("src/main/resources");
    private static final Path ASSET_ROOT = RESOURCE_ROOT.resolve("assets/realistic_ores");
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
            assertEquals("realistic_ores:block/" + blockId + "_" + index, entry.get("model").getAsString(), statePath.toString());
        }
    }

    private static void assertItemUsesCanonicalModel(String blockId) {
        Path itemPath = ASSET_ROOT.resolve("models/item/" + blockId + ".json");
        assertEquals("realistic_ores:block/" + blockId + "_0",
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
        return "realistic_ores:block/" + blockId + "_" + variant + "_" + face;
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
        Path definitions = resources.resolve("data/realistic_ores/realistic_ores");
        try (var paths = Files.list(definitions)) {
            for (Path path : paths.filter(file -> file.getFileName().toString().endsWith(".json")).toList()) {
                OreDefinition definition = read(path, OreDefinition.class);
                String crushed = definition.crushedItemId();
                String sample = definition.surfaceSampleBlockId();
                assertFalse(Files.exists(resources.resolve("assets/realistic_ores/blockstates/" + crushed + ".json")), crushed);
                assertFalse(Files.exists(resources.resolve("data/realistic_ores/loot_tables/blocks/" + crushed + ".json")), crushed);
                assertTrue(Files.isRegularFile(resources.resolve("assets/realistic_ores/blockstates/" + sample + ".json")), sample);
                assertTrue(Files.isRegularFile(resources.resolve("data/realistic_ores/loot_tables/blocks/" + sample + ".json")), sample);
                for (int variant = 0; variant < 5; variant++) {
                    Path modelPath = resources.resolve(
                            "assets/realistic_ores/models/block/" + sample + "_" + variant + ".json");
                    assertTrue(Files.isRegularFile(modelPath), sample);
                    assertSurfaceSampleModelUsesOpaqueOreTexture(resources, modelPath);
                }
                JsonObject itemModel = read(
                        resources.resolve("assets/realistic_ores/models/item/" + crushed + ".json"),
                        JsonObject.class);
                String texture = itemModel.getAsJsonObject("textures").get("layer0").getAsString();
                assertTrue(texture.startsWith("realistic_ores:item/"), crushed + " texture " + texture);
                assertTrue(Files.isRegularFile(resources.resolve(
                        "assets/realistic_ores/textures/item/" + texture.substring("realistic_ores:item/".length()) + ".png")));
                JsonObject sampleItemModel = read(
                        resources.resolve("assets/realistic_ores/models/item/" + sample + ".json"),
                        JsonObject.class);
                assertEquals("realistic_ores:block/" + sample + "_2",
                        sampleItemModel.get("parent").getAsString(), sample);
                JsonObject gui = sampleItemModel.getAsJsonObject("display").getAsJsonObject("gui");
                assertEquals(30, gui.getAsJsonArray("rotation").get(0).getAsInt(), sample);
                assertEquals(225, gui.getAsJsonArray("rotation").get(1).getAsInt(), sample);
                assertTrue(gui.getAsJsonArray("scale").get(0).getAsDouble() > 1.0, sample);
            }
        }
        assertTrue(Files.isRegularFile(resources.resolve("assets/realistic_ores/blockstates/oil_seep.json")));
        assertTrue(Files.isRegularFile(resources.resolve("data/realistic_ores/loot_tables/blocks/oil_seep.json")));
        for (int variant = 0; variant < 5; variant++) {
            JsonObject oilModel = read(resources.resolve(
                    "assets/realistic_ores/models/block/oil_seep_" + variant + ".json"), JsonObject.class);
            assertEquals("realistic_ores:block/oil_bearing_shale",
                    oilModel.getAsJsonObject("textures").get("all").getAsString());
            assertTrue(oilModel.getAsJsonArray("elements").size() >= 4);
            assertTrue(oilModel.getAsJsonArray("elements").size() <= 5);
        }
        Path oilTexture = resources.resolve("assets/realistic_ores/textures/block/oil_bearing_shale.png");
        BufferedImage oilImage = ImageIO.read(oilTexture.toFile());
        assertTrue(oilImage != null);
        assertEquals(16, oilImage.getWidth());
        assertEquals(16, oilImage.getHeight());
    }

    @Test
    void oreChunksHaveTransparentItemsAndEnchantmentStableLoot() throws IOException {
        Path resources = Path.of("src/main/resources");
        Path definitions = resources.resolve("data/realistic_ores/realistic_ores");
        int definitionCount = 0;
        try (var paths = Files.list(definitions)) {
            for (Path path : paths.filter(file -> file.getFileName().toString().endsWith(".json")).toList()) {
                definitionCount++;
                OreDefinition definition = read(path, OreDefinition.class);
                String chunk = definition.oreChunkItemId();
                Path texturePath = resources.resolve("assets/realistic_ores/textures/item/" + chunk + ".png");
                BufferedImage image = ImageIO.read(texturePath.toFile());
                assertTrue(image != null, texturePath.toString());
                assertEquals(16, image.getWidth(), texturePath.toString());
                assertEquals(16, image.getHeight(), texturePath.toString());
                boolean hasTransparentPixel = false;
                boolean hasVisiblePixel = false;
                for (int y = 0; y < image.getHeight(); y++) {
                    for (int x = 0; x < image.getWidth(); x++) {
                        int alpha = image.getRGB(x, y) >>> 24;
                        hasTransparentPixel |= alpha == 0;
                        hasVisiblePixel |= alpha > 0;
                    }
                }
                assertTrue(hasTransparentPixel, chunk + " must use a transparent background");
                assertTrue(hasVisiblePixel, chunk + " must contain visible ore pixels");

                JsonObject itemModel = read(
                        resources.resolve("assets/realistic_ores/models/item/" + chunk + ".json"),
                        JsonObject.class);
                assertEquals("minecraft:item/generated", itemModel.get("parent").getAsString(), chunk);
                assertEquals("realistic_ores:item/" + chunk,
                        itemModel.getAsJsonObject("textures").get("layer0").getAsString(), chunk);

                for (OreDefinition.VariantDefinition variant : definition.variants()) {
                    JsonObject loot = read(
                            resources.resolve("data/realistic_ores/loot_tables/blocks/" + variant.blockId() + ".json"),
                            JsonObject.class);
                    String serialized = GSON.toJson(loot);
                    assertTrue(serialized.contains("minecraft:silk_touch"), variant.blockId());
                    assertTrue(serialized.contains("realistic_ores:" + chunk), variant.blockId());
                    assertTrue(serialized.contains("realistic_ores:" + variant.blockId()), variant.blockId());
                    assertFalse(serialized.contains("minecraft:fortune"), variant.blockId());
                    assertFalse(serialized.contains("apply_bonus"), variant.blockId());

                    assertTrue(Files.isRegularFile(resources.resolve(
                            "data/realistic_ores/recipes/crafting/ore_reassembly/" + variant.blockId() + ".json")));
                    assertTrue(Files.isRegularFile(resources.resolve(
                            "data/realistic_ores/recipes/compat/create/crushing/" + variant.blockId() + ".json")));
                }
                assertTrue(Files.isRegularFile(resources.resolve(
                        "data/realistic_ores/recipes/compat/create/crushing/ore_chunks/"
                                + definition.primaryVariant().blockId() + ".json")));
            }
        }
        assertEquals(22, definitionCount);
    }

    private static void assertSurfaceSampleModelUsesOpaqueOreTexture(Path resources, Path modelPath) throws IOException {
        JsonObject model = read(modelPath, JsonObject.class);
        assertEquals("#all", model.getAsJsonObject("textures").get("particle").getAsString(), modelPath.toString());
        String texture = model.getAsJsonObject("textures").get("all").getAsString();
        assertTrue(texture.startsWith("realistic_ores:block/"), modelPath + " texture " + texture);
        Path texturePath = resources.resolve(
                "assets/realistic_ores/textures/block/" + texture.substring("realistic_ores:block/".length()) + ".png");
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
            JsonArray from = element.getAsJsonObject().getAsJsonArray("from");
            JsonArray to = element.getAsJsonObject().getAsJsonArray("to");
            assertTrue(from.get(0).getAsDouble() >= 0 && from.get(2).getAsDouble() >= 0, modelPath.toString());
            assertTrue(to.get(0).getAsDouble() <= 16 && to.get(2).getAsDouble() <= 16, modelPath.toString());
            assertTrue(from.get(1).getAsDouble() > 0, modelPath.toString());
            assertTrue(to.get(1).getAsDouble() >= 2 && to.get(1).getAsDouble() <= 4, modelPath.toString());
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
        assertTrue(model.getAsJsonArray("elements").size() >= 3, modelPath.toString());
        assertTrue(model.getAsJsonArray("elements").size() <= 4, modelPath.toString());
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
