package io.github.realisticores.ore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class OreDefinition {
    private String id;
    private String display_name;
    private List<VariantDefinition> variants = List.of();
    private List<String> tags = List.of();

    public String id() {
        return id;
    }

    public String displayName() {
        return display_name;
    }

    public List<VariantDefinition> variants() {
        return variants;
    }

    public List<String> tags() {
        return tags;
    }

    public void validate() {
        require(id, "id");
        require(display_name, "display_name");
        if (variants == null || variants.isEmpty()) {
            throw new IllegalArgumentException("Ore " + id + " must define at least one variant");
        }

        List<String> seenHosts = new ArrayList<>();
        for (VariantDefinition variant : variants) {
            variant.validate(id);
            if (seenHosts.contains(variant.host())) {
                throw new IllegalArgumentException("Ore " + id + " defines duplicate host " + variant.host());
            }
            seenHosts.add(variant.host());
        }

        if (tags == null) {
            tags = List.of();
        }
    }

    public Optional<VariantDefinition> variantByHost(String host) {
        return variants.stream().filter(variant -> variant.host().equals(host)).findFirst();
    }

    private static void require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
    }

    public static final class VariantDefinition {
        private String host;
        private String block_id;
        private String texture_mode;
        private TexturesDefinition textures;
        private String copy_properties_from;

        public String host() {
            return host;
        }

        public String blockId() {
            return block_id;
        }

        public TextureMode textureMode() {
            return TextureMode.fromSerialized(texture_mode);
        }

        public TexturesDefinition textures() {
            return textures;
        }

        public String copyPropertiesFrom() {
            return copy_properties_from;
        }

        private void validate(String oreId) {
            require(host, "variants.host");
            require(block_id, "variants.block_id");
            require(texture_mode, "variants.texture_mode");
            require(copy_properties_from, "variants.copy_properties_from");
            Objects.requireNonNull(textures, "variants.textures");
            textureMode().validateTextures(textures);
        }
    }

    public static final class TexturesDefinition {
        private String all;
        private String side;
        private String top;
        private String bottom;

        public String all() {
            return all;
        }

        public String side() {
            return side;
        }

        public String top() {
            return top;
        }

        public String bottom() {
            return bottom;
        }
    }

    public enum TextureMode {
        CUBE_ALL,
        CUBE_COLUMN_LIKE;

        public static TextureMode fromSerialized(String value) {
            return switch (value.toLowerCase(Locale.ROOT)) {
                case "cube_all" -> CUBE_ALL;
                case "cube_column_like" -> CUBE_COLUMN_LIKE;
                default -> throw new IllegalArgumentException("Unsupported texture_mode: " + value);
            };
        }

        public void validateTextures(TexturesDefinition textures) {
            switch (this) {
                case CUBE_ALL -> require(textures.all(), "textures.all");
                case CUBE_COLUMN_LIKE -> {
                    require(textures.side(), "textures.side");
                    require(textures.top(), "textures.top");
                    require(textures.bottom(), "textures.bottom");
                }
            }
        }
    }
}
