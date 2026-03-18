package io.github.realisticores.ore;

import java.util.List;

public final class WorldgenDefinitionLoader {
    private WorldgenDefinitionLoader() {
    }

    public static List<WorldgenDefinition> loadAll() {
        return OreDefinitionLoader.loadDirectory("realistic_ore_generation", WorldgenDefinition.class).stream()
                .peek(WorldgenDefinition::validate)
                .toList();
    }
}
