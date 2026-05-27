package io.github.realisticores.ore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

final class OreDefinitionTest {
    private static final Gson GSON = new Gson();

    @Test
    void validatesCubeAllDefinitionAndDerivesPrimaryCrushedItem() {
        OreDefinition definition = GSON.fromJson("""
                {
                  "id": "test_ore",
                  "display_name": "Test Ore",
                  "variants": [
                    {
                      "host": "stone",
                      "block_id": "test_ore",
                      "texture_mode": "cube_all",
                      "textures": { "all": "realisticores:block/test_ore" },
                      "copy_properties_from": "minecraft:stone"
                    }
                  ]
                }
                """, OreDefinition.class);

        definition.validate();

        assertEquals("test_ore", definition.primaryVariant().blockId());
        assertEquals("crushed_test_ore", definition.crushedItemId());
        assertEquals("stone", definition.primaryVariant().host());
    }

    @Test
    void rejectsDuplicateVariantHosts() {
        OreDefinition definition = GSON.fromJson("""
                {
                  "id": "test_ore",
                  "display_name": "Test Ore",
                  "variants": [
                    {
                      "host": "stone",
                      "block_id": "test_ore",
                      "texture_mode": "cube_all",
                      "textures": { "all": "realisticores:block/test_ore" },
                      "copy_properties_from": "minecraft:stone"
                    },
                    {
                      "host": "stone",
                      "block_id": "test_ore_duplicate",
                      "texture_mode": "cube_all",
                      "textures": { "all": "realisticores:block/test_ore_duplicate" },
                      "copy_properties_from": "minecraft:stone"
                    }
                  ]
                }
                """, OreDefinition.class);

        assertThrows(IllegalArgumentException.class, definition::validate);
    }

    @Test
    void rejectsColumnTexturesWithoutAllRequiredFaces() {
        OreDefinition definition = GSON.fromJson("""
                {
                  "id": "test_ore",
                  "display_name": "Test Ore",
                  "variants": [
                    {
                      "host": "deepslate",
                      "block_id": "deepslate_test_ore",
                      "texture_mode": "cube_column_like",
                      "textures": {
                        "side": "realisticores:block/deepslate_test_ore_side",
                        "top": "realisticores:block/deepslate_test_ore_top"
                      },
                      "copy_properties_from": "minecraft:deepslate"
                    }
                  ]
                }
                """, OreDefinition.class);

        assertThrows(IllegalArgumentException.class, definition::validate);
    }
}
