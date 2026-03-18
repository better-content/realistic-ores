package io.github.realisticores.ore;

import java.util.Locale;

public final class WorldgenDefinition {
    private String ore_id;
    private String variant;
    private String target_tag;
    private int vein_size;
    private int count_per_chunk;
    private String distribution;
    private int min_y;
    private int max_y;
    private String biome_filter;
    private String generation_step;
    private Boolean enabled;
    private Float discard_chance_on_air_exposure;

    public String oreId() {
        return ore_id;
    }

    public String variant() {
        return variant;
    }

    public String targetTag() {
        return target_tag;
    }

    public int veinSize() {
        return vein_size;
    }

    public int countPerChunk() {
        return count_per_chunk;
    }

    public Distribution distribution() {
        return Distribution.fromSerialized(distribution);
    }

    public int minY() {
        return min_y;
    }

    public int maxY() {
        return max_y;
    }

    public String biomeFilter() {
        return biome_filter == null || biome_filter.isBlank() ? "#minecraft:is_overworld" : biome_filter;
    }

    public String generationStep() {
        return generation_step == null || generation_step.isBlank() ? "underground_ores" : generation_step;
    }

    public boolean enabled() {
        return enabled == null || enabled;
    }

    public float discardChanceOnAirExposure() {
        return discard_chance_on_air_exposure == null ? 0.0F : discard_chance_on_air_exposure;
    }

    public String registryId() {
        return ore_id + "_" + variant;
    }

    public void validate() {
        require(ore_id, "ore_id");
        require(variant, "variant");
        require(target_tag, "target_tag");
        require(distribution, "distribution");
        if (vein_size < 1) {
            throw new IllegalArgumentException("vein_size must be >= 1 for " + registryId());
        }
        if (count_per_chunk < 0) {
            throw new IllegalArgumentException("count_per_chunk must be >= 0 for " + registryId());
        }
        if (max_y < min_y) {
            throw new IllegalArgumentException("max_y must be >= min_y for " + registryId());
        }
        distribution();
    }

    private static void require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
    }

    public enum Distribution {
        UNIFORM,
        TRIANGLE;

        public static Distribution fromSerialized(String value) {
            return switch (value.toLowerCase(Locale.ROOT)) {
                case "uniform" -> UNIFORM;
                case "triangle" -> TRIANGLE;
                default -> throw new IllegalArgumentException("Unsupported distribution: " + value);
            };
        }
    }
}
