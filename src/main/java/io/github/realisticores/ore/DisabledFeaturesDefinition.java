package io.github.realisticores.ore;

import java.util.List;

public final class DisabledFeaturesDefinition {
    private List<String> features = List.of();

    public List<String> features() {
        return features;
    }

    public void validate() {
        if (features == null) {
            throw new IllegalArgumentException("features must be present");
        }
    }
}
