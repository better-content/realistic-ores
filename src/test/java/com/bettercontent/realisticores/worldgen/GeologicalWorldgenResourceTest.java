package com.bettercontent.realisticores.worldgen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GeologicalWorldgenResourceTest {
    private static final Path DATA = Path.of("src/main/resources/data/realistic_ores");

    @Test void resourcesUseOneDualHostConfigurationAndTwoAltitudeProfilesPerFamily() throws Exception {
        JsonObject manifest = read(Path.of("tools/geological_worldgen.json"));
        assertEquals(8, manifest.size());
        assertFalse(Files.exists(DATA.resolve("realistic_ore_generation")));
        assertEquals(manifest.keySet(), stems(DATA.resolve("worldgen/configured_feature")));
        assertEquals(17, stems(DATA.resolve("worldgen/placed_feature")).size());

        for (String family : manifest.keySet()) {
            JsonObject definition = manifest.getAsJsonObject(family);
            JsonObject configured = read(DATA.resolve("worldgen/configured_feature/" + family + ".json"));
            assertEquals("realistic_ores:geological_deposit", configured.get("type").getAsString());
            JsonObject config = configured.getAsJsonObject("config");
            assertEquals(definition.get("morphology"), config.get("morphology"));
            assertEquals(2, config.getAsJsonArray("targets").size());
            assertEquals("minecraft:stone_ore_replaceables", targetTag(config, 0));
            assertEquals("minecraft:deepslate_ore_replaceables", targetTag(config, 1));
            assertEquals(definition.get("home_budget"), config.get("block_budget"));
            assertEquals(definition.get("home_spread"), config.get("budget_spread"));
            assertEquals("home", config.get("deposit_class").getAsString());

            assertProfile(family, "home", "minecraft:trapezoid", definition);
            assertProfile(family, "echo", "minecraft:uniform", definition);
            JsonObject echo = read(DATA.resolve("worldgen/placed_feature/" + family + "_echo.json"))
                    .getAsJsonObject("feature").getAsJsonObject("config");
            assertEquals(definition.get("echo_budget"), echo.get("block_budget"));
            assertEquals(definition.get("echo_spread"), echo.get("budget_spread"));
            assertEquals("echo", echo.get("deposit_class").getAsString());
            assertSupplyEnvelope(family, definition);
        }
    }

    @Test void depositsUseApprovedDimensionHostMappings() throws Exception {
        try (var modifiers = Files.list(DATA.resolve("forge/biome_modifier"))) {
            for (Path path : modifiers.filter(path -> path.getFileName().toString().startsWith("add_")).toList()) {
                String name = path.getFileName().toString();
                String biomes = read(path).get("biomes").getAsString();
                if (name.equals("add_ironstone_aether.json")) {
                    assertEquals("#aether:is_aether", biomes);
                } else {
                    assertEquals("#minecraft:is_overworld", biomes, path.toString());
                }
            }
        }

        JsonObject aether = read(DATA.resolve("worldgen/placed_feature/ironstone_aether.json"));
        JsonObject config = aether.getAsJsonObject("feature").getAsJsonObject("config");
        assertEquals("realistic_ores:geological_deposit", aether.getAsJsonObject("feature").get("type").getAsString());
        assertEquals("realistic_ores:aether_holystone", targetTag(config, 0));
        assertEquals("realistic_ores:ironstone", config.getAsJsonArray("targets").get(0).getAsJsonObject()
                .getAsJsonObject("state").get("Name").getAsString());
        assertEquals("lenticular_oolitic_bed", config.get("morphology").getAsString());
        assertEquals("echo", config.get("deposit_class").getAsString());
    }

    private static void assertProfile(String family, String profile, String distribution, JsonObject definition) {
        JsonObject placed = read(DATA.resolve("worldgen/placed_feature/" + family + "_" + profile + ".json"));
        var placement = placed.getAsJsonArray("placement");
        JsonObject range = placement.get(2).getAsJsonObject().getAsJsonObject("height");
        assertEquals(distribution, range.get("type").getAsString(), family + " " + profile);
        var band = definition.getAsJsonArray(profile);
        assertEquals(band.get(0).getAsInt(), range.getAsJsonObject("min_inclusive").get("absolute").getAsInt());
        assertEquals(band.get(1).getAsInt(), range.getAsJsonObject("max_inclusive").get("absolute").getAsInt());
        assertTrue(band.get(0).getAsInt() >= -128 && band.get(1).getAsInt() <= 512);
    }

    private static void assertSupplyEnvelope(String family, JsonObject definition) {
        double home = definition.get("home_count").getAsDouble() * definition.get("home_budget").getAsDouble();
        double echoFrequency = definition.has("echo_count")
                ? definition.get("echo_count").getAsDouble()
                : 1.0 / definition.get("echo_rarity").getAsDouble();
        double echo = echoFrequency * definition.get("echo_budget").getAsDouble();
        double baseline = definition.get("baseline_supply").getAsDouble();
        assertTrue(Math.abs((home + echo) / baseline - 1.0) <= 0.040001, family + " changed supply");
        double share = echo / (home + echo);
        if (family.equals("hotstone")) {
            assertTrue(share >= 0.04 && share <= 0.06, "hotstone echo share " + share);
        } else {
            assertTrue(share >= 0.10 && share <= 0.20, family + " echo share " + share);
        }
        assertTrue(definition.get("echo_budget").getAsInt() > definition.get("home_budget").getAsInt(),
                family + " echo bodies must be larger");
    }

    private static String targetTag(JsonObject config, int index) {
        return config.getAsJsonArray("targets").get(index).getAsJsonObject()
                .getAsJsonObject("target").get("tag").getAsString();
    }

    private static Set<String> stems(Path directory) throws IOException {
        try (var paths = Files.list(directory)) {
            return paths.filter(path -> path.toString().endsWith(".json"))
                    .map(path -> path.getFileName().toString().replaceFirst("\\.json$", ""))
                    .collect(Collectors.toUnmodifiableSet());
        }
    }

    private static JsonObject read(Path path) {
        try {
            return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        } catch (IOException exception) {
            throw new IllegalStateException(path.toString(), exception);
        }
    }
}
