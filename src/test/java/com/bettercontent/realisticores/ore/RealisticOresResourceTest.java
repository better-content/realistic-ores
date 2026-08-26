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
import java.util.List;
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
    private static final Set<String> SALIENT_FAMILIES = Set.of(
            "coal_measures", "ironstone", "copper_bloom", "tin_quartz", "brassroot",
            "evaporite_beds", "hotstone", "black_shale");
    private static final Set<String> RETAINED_MATERIALS = Set.of(
            "aluminum", "amethyst", "cadmium", "coal", "cobalt", "copper", "diamond",
            "emerald", "gold", "iron", "lapis", "lead", "nickel", "osmium", "quartz",
            "redstone", "rock_salt", "saltpeter", "silver", "sodium_chloride", "soul_sand",
            "sulfur", "thorium", "tin", "titanium", "uranium", "zinc");

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
        JsonObject palettes = read(Path.of("tools/ore_art_manifest.json"), JsonObject.class);
        JsonObject canonicalHashes = read(Path.of("src/test/resources/canonical_ore_texture_hashes.json"), JsonObject.class);

        try (var paths = Files.list(DATA_ROOT.resolve("realistic_ores"))) {
            for (Path path : paths.filter(file -> file.getFileName().toString().endsWith(".json")).toList()) {
                OreDefinition definition = read(path, OreDefinition.class);
                definition.validate();
                Set<Integer> palette = new HashSet<>();
                palettes.getAsJsonObject(definition.id()).getAsJsonArray("palette")
                        .forEach(color -> palette.add(parseRgb(color.getAsString())));

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
        return variant == 0
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
        return family;
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
                String small = definition.smallOreChunkItemId();
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
                        resources.resolve("assets/realistic_ores/models/item/" + small + ".json"),
                        JsonObject.class);
                assertEquals("minecraft:item/generated",
                        sampleItemModel.get("parent").getAsString(), sample);
                assertEquals("realistic_ores:item/" + small,
                        sampleItemModel.getAsJsonObject("textures").get("layer0").getAsString(), sample);
                Path smallTexture = resources.resolve("assets/realistic_ores/textures/item/" + small + ".png");
                BufferedImage smallImage = ImageIO.read(smallTexture.toFile());
                assertTrue(smallImage != null, smallTexture.toString());
                assertEquals(16, smallImage.getWidth(), smallTexture.toString());
                assertEquals(16, smallImage.getHeight(), smallTexture.toString());
                assertFalse(Files.exists(resources.resolve("assets/realistic_ores/models/item/" + sample + ".json")),
                        "surface samples have no separate item identity");
                JsonObject sampleLoot = read(resources.resolve(
                        "data/realistic_ores/loot_tables/blocks/" + sample + ".json"), JsonObject.class);
                String serializedLoot = GSON.toJson(sampleLoot);
                assertTrue(serializedLoot.contains("realistic_ores:" + small));
                assertFalse(serializedLoot.contains("fortune"));
                JsonObject combine = read(resources.resolve(
                        "data/realistic_ores/recipes/crafting/small_chunks/" + definition.id() + ".json"),
                        JsonObject.class);
                assertEquals(9, combine.getAsJsonArray("ingredients").size());
                assertEquals("realistic_ores:" + definition.oreChunkItemId(),
                        combine.getAsJsonObject("result").get("item").getAsString());
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
                String familyTag = definition.id();
                assertDepositTags(resources, definition, familyTag, chunk);
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

                    Path reassemblyPath = resources.resolve(
                            "data/realistic_ores/recipes/crafting/ore_reassembly/" + variant.blockId() + ".json");
                    JsonObject reassembly = read(reassemblyPath, JsonObject.class);
                    assertEquals("minecraft:crafting_shapeless", reassembly.get("type").getAsString(),
                            reassemblyPath.toString());
                    assertEquals(Set.of("realistic_ores:" + chunk, variant.copyPropertiesFrom()),
                            reassembly.getAsJsonArray("ingredients").asList().stream()
                                    .map(ingredient -> ingredient.getAsJsonObject().get("item").getAsString())
                                    .collect(Collectors.toUnmodifiableSet()),
                            reassemblyPath.toString());
                    assertEquals("realistic_ores:" + variant.blockId(),
                            reassembly.getAsJsonObject("result").get("item").getAsString(),
                            reassemblyPath.toString());

                    Path blockCrushingPath = resources.resolve(
                            "data/realistic_ores/recipes/compat/create/crushing/" + variant.blockId() + ".json");
                    JsonObject blockCrushing = read(blockCrushingPath, JsonObject.class);
                    assertEquals("create:crushing", blockCrushing.get("type").getAsString(),
                            blockCrushingPath.toString());
                    JsonArray blockIngredients = blockCrushing.getAsJsonArray("ingredients");
                    assertEquals(1, blockIngredients.size(), blockCrushingPath.toString());
                    JsonObject blockIngredient = blockIngredients.get(0).getAsJsonObject();
                    assertEquals("forge:nbt", blockIngredient.get("type").getAsString(),
                            blockCrushingPath.toString());
                    assertEquals("realistic_ores:" + variant.blockId(), blockIngredient.get("item").getAsString(),
                            blockCrushingPath.toString());
                    JsonArray blockResults = blockCrushing.getAsJsonArray("results");
                    assertEquals(2, blockResults.size(), blockCrushingPath.toString());
                    assertEquals(Set.of("item"), blockResults.get(0).getAsJsonObject().keySet(),
                            blockCrushingPath.toString());
                    assertEquals("realistic_ores:" + chunk,
                            blockResults.get(0).getAsJsonObject().get("item").getAsString(),
                            blockCrushingPath.toString());
                    assertEquals(Set.of("item"), blockResults.get(1).getAsJsonObject().keySet(),
                            blockCrushingPath.toString());
                    assertEquals(variant.copyPropertiesFrom(),
                            blockResults.get(1).getAsJsonObject().get("item").getAsString(),
                            blockCrushingPath.toString());
                }

                Path chunkCrushingPath = resources.resolve(
                            "data/realistic_ores/recipes/compat/create/crushing/ore_chunks/"
                                + definition.id() + ".json");
                JsonObject chunkCrushing = read(chunkCrushingPath, JsonObject.class);
                assertEquals("create:crushing", chunkCrushing.get("type").getAsString(),
                        chunkCrushingPath.toString());
                JsonArray chunkIngredients = chunkCrushing.getAsJsonArray("ingredients");
                assertEquals(1, chunkIngredients.size(), chunkCrushingPath.toString());
                assertEquals("realistic_ores:" + chunk,
                        chunkIngredients.get(0).getAsJsonObject().get("item").getAsString(),
                        chunkCrushingPath.toString());
                JsonArray chunkResults = chunkCrushing.getAsJsonArray("results");
                assertEquals(1, chunkResults.size(), chunkCrushingPath.toString());
                assertEquals(Set.of("item", "count"), chunkResults.get(0).getAsJsonObject().keySet(),
                        chunkCrushingPath.toString());
                assertEquals("realistic_ores:" + definition.crushedItemId(),
                        chunkResults.get(0).getAsJsonObject().get("item").getAsString(),
                        chunkCrushingPath.toString());
                assertEquals(3, chunkResults.get(0).getAsJsonObject().get("count").getAsInt(),
                        chunkCrushingPath.toString());

                Path chunkMillingPath = resources.resolve(
                            "data/realistic_ores/recipes/compat/create/milling/ore_chunks/"
                                + definition.id() + ".json");
                JsonObject chunkMilling = read(chunkMillingPath, JsonObject.class);
                assertEquals("create:milling", chunkMilling.get("type").getAsString(),
                        chunkMillingPath.toString());
                assertEquals("forge:mod_loaded",
                        chunkMilling.getAsJsonArray("conditions").get(0).getAsJsonObject().get("type").getAsString(),
                        chunkMillingPath.toString());
                assertEquals("create",
                        chunkMilling.getAsJsonArray("conditions").get(0).getAsJsonObject().get("modid").getAsString(),
                        chunkMillingPath.toString());
                assertEquals(400, chunkMilling.get("processingTime").getAsInt(), chunkMillingPath.toString());
                JsonArray millingIngredients = chunkMilling.getAsJsonArray("ingredients");
                assertEquals(1, millingIngredients.size(), chunkMillingPath.toString());
                assertEquals("realistic_ores:" + chunk,
                        millingIngredients.get(0).getAsJsonObject().get("item").getAsString(),
                        chunkMillingPath.toString());
                JsonArray millingResults = chunkMilling.getAsJsonArray("results");
                assertEquals(1, millingResults.size(), chunkMillingPath.toString());
                assertEquals(Set.of("item", "count"), millingResults.get(0).getAsJsonObject().keySet(),
                        chunkMillingPath.toString());
                assertEquals("realistic_ores:" + definition.crushedItemId(),
                        millingResults.get(0).getAsJsonObject().get("item").getAsString(),
                        chunkMillingPath.toString());
                assertEquals(2, millingResults.get(0).getAsJsonObject().get("count").getAsInt(),
                        chunkMillingPath.toString());

                for (String recipeType : List.of("furnace", "blasting")) {
                    Path chunkCookPath = resources.resolve("data/realistic_ores/recipes/thermal/"
                            + recipeType + "/" + definition.id() + "_chunk.json");
                    Path crushedCookPath = resources.resolve("data/realistic_ores/recipes/thermal/"
                            + recipeType + "/" + definition.id() + "_crushed.json");
                    assertEquals(2, read(chunkCookPath, JsonObject.class)
                            .getAsJsonObject("result").get("count").getAsInt(), chunkCookPath.toString());
                    assertEquals(1, read(crushedCookPath, JsonObject.class)
                            .getAsJsonObject("result").get("count").getAsInt(), crushedCookPath.toString());
                }
            }
        }
        assertEquals(8, definitionCount);
        Path chunkMillingDirectory = resources.resolve(
                "data/realistic_ores/recipes/compat/create/milling/ore_chunks");
        try (var millingPaths = Files.list(chunkMillingDirectory)) {
            assertEquals(8, millingPaths.filter(path -> path.getFileName().toString().endsWith(".json")).count(),
                    chunkMillingDirectory.toString());
        }
    }

    private static void assertDepositTags(
            Path resources,
            OreDefinition definition,
            String familyTag,
            String chunk
    ) {
        Set<String> expectedBlocks = definition.variants().stream()
                .map(variant -> "realistic_ores:" + variant.blockId())
                .collect(Collectors.toUnmodifiableSet());
        Path path = resources.resolve("data/realistic_ores/tags/blocks/deposit_ore_blocks/"
                + familyTag + ".json");
        JsonObject tag = read(path, JsonObject.class);
        Set<String> values = tag.getAsJsonArray("values").asList().stream()
                .map(entry -> entry.getAsString())
                .collect(Collectors.toUnmodifiableSet());
        assertEquals(expectedBlocks, values, path.toString());
        assertFalse(Files.exists(resources.resolve("data/realistic_ores/tags/items/deposit_ore_blocks/"
                + familyTag + ".json")), "hosted ore blocks must not be exposed through processing item tags");

        Path chunkPath = resources.resolve("data/realistic_ores/tags/items/deposit_chunks/"
                + familyTag + ".json");
        JsonObject chunkTag = read(chunkPath, JsonObject.class);
        assertEquals(List.of("realistic_ores:" + chunk),
                chunkTag.getAsJsonArray("values").asList().stream().map(entry -> entry.getAsString()).toList(),
                chunkPath.toString());
    }

    @Test
    void phaseThreeProcessingGraphIsCompleteAndLegacyFree() throws IOException {
        Path processing = DATA_ROOT.resolve("processing_definitions");
        List<Path> definitions;
        try (var paths = Files.list(processing)) {
            definitions = paths.filter(path -> path.toString().endsWith(".json")).toList();
        }
        assertEquals(8, definitions.size());
        assertEquals(SALIENT_FAMILIES, definitions.stream()
                .map(path -> path.getFileName().toString().replace(".json", ""))
                .collect(Collectors.toUnmodifiableSet()));
        assertEquals(3, read(processing.resolve("hotstone.json"), JsonObject.class)
                .getAsJsonArray("assay_variants").size());
        int routeCount = 0;
        int consumedMediumRoutes = 0;
        Set<String> media = new HashSet<>();
        for (Path path : definitions) {
            JsonObject definition = read(path, JsonObject.class);
            JsonArray routes = definition.getAsJsonArray("routes");
            assertTrue(routes.size() >= 2 && routes.size() <= 5, path.toString());
            routeCount += routes.size();
            for (var routeElement : routes) {
                JsonObject route = routeElement.getAsJsonObject();
                media.add(route.get("medium").getAsString());
                JsonArray fluids = route.getAsJsonArray("fluids");
                assertTrue(fluids.size() == 1 || fluids.size() == 2, path.toString());
                int amount = fluids.asList().stream()
                        .mapToInt(fluid -> fluid.getAsJsonObject().get("amount").getAsInt()).sum();
                assertEquals(500, amount, path.toString());
                double returnChance = route.get("ball_return_chance").getAsDouble();
                if (returnChance == 0.0) {
                    consumedMediumRoutes++;
                } else {
                    assertTrue(returnChance >= .80);
                    assertTrue(returnChance <= .98);
                }
            }
        }
        assertEquals(23, routeCount);
        assertEquals(4, consumedMediumRoutes);
        assertEquals(Set.of("andesite", "iron", "brass", "steel", "nickel", "titanium",
                "blood_infused", "fluix"), media);

        Path separation = DATA_ROOT.resolve("recipes/compat/create/separation");
        Set<String> createConcentrates = new HashSet<>();
        try (var paths = Files.list(separation)) {
            List<Path> recipes = paths.filter(path -> path.toString().endsWith(".json")).toList();
            assertEquals(23, recipes.size());
            for (Path path : recipes) {
                JsonObject recipe = read(path, JsonObject.class);
                assertEquals("create:mixing", recipe.get("type").getAsString());
                long crushed = recipe.getAsJsonArray("ingredients").asList().stream()
                        .filter(value -> value.getAsJsonObject().has("item"))
                        .filter(value -> value.getAsJsonObject().get("item").getAsString().contains(":crushed_"))
                        .count();
                assertEquals(4, crushed, path.toString());
                JsonArray results = recipe.getAsJsonArray("results");
                assertTrue(results.size() <= 4, path + " exceeds Create's four-item output limit");
                results.forEach(value -> {
                    JsonObject result = value.getAsJsonObject();
                    if (result.has("item") && result.get("item").getAsString().endsWith("_concentrate")) {
                        String item = result.get("item").getAsString();
                        createConcentrates.add(item.substring("realistic_ores:".length(),
                                item.length() - "_concentrate".length()));
                    }
                });
                assertFalse(GSON.toJson(recipe).contains("tailings"));
                assertFalse(GSON.toJson(recipe).contains("washed_"));
            }
        }
        assertEquals(RETAINED_MATERIALS, createConcentrates,
                "Create separation must retain the complete audited material catalogue");

        assertTrue(Files.exists(DATA_ROOT.resolve("recipes/thermal/furnace/copper_bloom_chunk.json")));
        assertTrue(Files.exists(DATA_ROOT.resolve("recipes/compat/tconstruct/melting/copper_bloom_chunk.json")));
        for (String material : List.of("quartz", "diamond", "emerald", "amethyst")) {
            assertFalse(Files.exists(DATA_ROOT.resolve(
                    "recipes/compat/tconstruct/melting/concentrate_" + material + ".json")), material);
            assertFalse(Files.exists(DATA_ROOT.resolve(
                    "recipes/compat/tconstruct/foundry/concentrate_" + material + ".json")), material);
        }
        for (String immediate : List.of("evaporite_rock_salt", "black_shale_soul_sand",
                "hotstone_magma")) {
            assertTrue(Files.exists(DATA_ROOT.resolve("recipes/crafting/immediate/" + immediate + ".json")),
                    immediate);
        }
        for (String material : List.of("titanium", "thorium")) {
            JsonObject moltenTag = read(RESOURCE_ROOT.resolve(
                    "data/forge/tags/fluids/molten_" + material + ".json"), JsonObject.class);
            assertEquals(2, moltenTag.getAsJsonArray("values").size());
            assertTrue(Files.isRegularFile(ASSET_ROOT.resolve(
                    "models/item/molten_" + material + "_bucket.json")));
            JsonObject blockstate = read(ASSET_ROOT.resolve(
                    "blockstates/molten_" + material + ".json"), JsonObject.class);
            assertEquals("realistic_ores:block/molten_" + material,
                    blockstate.getAsJsonObject("variants").getAsJsonObject("")
                            .get("model").getAsString());
            JsonObject blockModel = read(ASSET_ROOT.resolve(
                    "models/block/molten_" + material + ".json"), JsonObject.class);
            assertEquals("minecraft:block/water_still",
                    blockModel.getAsJsonObject("textures").get("particle").getAsString());
        }
        for (String material : List.of("beryl", "beryllium", "calcium", "carbon", "chromium", "gallium",
                "iridium", "magnesium", "phosphate", "platinum", "silicon", "sodium", "tantalum", "tungsten")) {
            assertFalse(Files.exists(DATA_ROOT.resolve(
                    "recipes/compat/tconstruct/melting/concentrate_" + material + ".json")),
                    "do not invent an uninstalled molten form for " + material);
            assertFalse(Files.exists(ASSET_ROOT.resolve("models/item/" + material + "_concentrate.json")),
                    "pruned concentrate remains in item resources: " + material);
        }
        try (var paths = Files.list(ASSET_ROOT.resolve("models/item"))) {
            assertEquals(RETAINED_MATERIALS, paths
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith("_concentrate.json"))
                    .map(name -> name.replace("_concentrate.json", ""))
                    .collect(Collectors.toUnmodifiableSet()));
        }

        try (var paths = Files.walk(RESOURCE_ROOT.resolve("data/realistic_ores"))) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                String content = Files.readString(path);
                assertFalse(content.contains("corundum_beryl"), path.toString());
                for (String obsolete : List.of("copper_sulfide", "tin_tungsten_greisen", "lead_zinc_vein",
                        "cupriferous_redbed", "phosphate_rock", "kimberlite_pipe", "uranium_ore",
                        "thorium_ore", "soul_bearing_black_shale", "gem_pipe", "redbed")) {
                    assertFalse(content.contains(obsolete), path + " retains obsolete family " + obsolete);
                }
                assertFalse(content.contains("washed_"), path.toString());
                assertFalse(content.contains("tailings"), path.toString());
            }
        }
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
