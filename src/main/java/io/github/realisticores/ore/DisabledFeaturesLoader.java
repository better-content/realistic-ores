package io.github.realisticores.ore;

import java.util.LinkedHashSet;
import java.util.Set;

public final class DisabledFeaturesLoader {
    private DisabledFeaturesLoader() {
    }

    public static Set<String> loadAll() {
        Set<String> features = new LinkedHashSet<>();
        for (DisabledFeaturesDefinition definition : OreDefinitionLoader.loadDirectory("disabled_placed_features", DisabledFeaturesDefinition.class)) {
            definition.validate();
            features.addAll(definition.features());
        }
        return Set.copyOf(features);
    }
}
