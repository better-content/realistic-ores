package com.bettercontent.realisticores.ore;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class OreMorphologyContractTest {
    private static final Path TEXTURES = Path.of("src/main/resources/assets/realistic_ores/textures/block");
    private static final Map<String, String> MORPHOLOGIES = Map.of(
        "hotstone", "centered_radial_starburst",
        "copper_bloom", "compact_paired_nodules",
        "tin_quartz", "parallel_stepped_crystal_ribbons",
        "brassroot", "asymmetric_directional_y_roots",
        "coal_measures", "broad_broken_horizontal_seams",
        "ironstone", "thick_blocky_brace_bands",
        "evaporite_beds", "upright_prismatic_crowns_from_thin_beds",
        "black_shale", "sparse_converging_angular_fractures");

    @Test void manifestLocksTheEightNonColorMorphologies() throws Exception {
        JsonObject manifest = JsonParser.parseString(Files.readString(Path.of("tools/ore_art_manifest.json"))).getAsJsonObject();
        assertEquals(MORPHOLOGIES.keySet(), manifest.keySet());
        MORPHOLOGIES.forEach((family, morphology) ->
            assertEquals(morphology, manifest.getAsJsonObject(family).get("morphology").getAsString(), family));
    }

    @Test void canonicalFacesHaveDistinctSilhouettesAndBoundedMineralCoverage() throws Exception {
        JsonObject manifest = JsonParser.parseString(Files.readString(Path.of("tools/ore_art_manifest.json"))).getAsJsonObject();
        Map<String, boolean[]> masks = new LinkedHashMap<>();
        for (String family : MORPHOLOGIES.keySet()) {
            Set<Integer> colors = new HashSet<>();
            manifest.getAsJsonObject(family).getAsJsonArray("palette")
                .forEach(color -> colors.add(Integer.parseInt(color.getAsString().substring(1), 16)));
            var image = ImageIO.read(TEXTURES.resolve(family + "_0_up.png").toFile());
            boolean[] mask = new boolean[256];
            int count = 0;
            for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
                mask[y * 16 + x] = colors.contains(image.getRGB(x, y) & 0xffffff);
                if (mask[y * 16 + x]) count++;
            }
            assertTrue(count >= 20 && count <= 33, family + " mineral coverage " + count);
            masks.put(family, mask);
        }
        Set<Integer> silhouettes = new HashSet<>();
        masks.forEach((family, mask) -> assertTrue(silhouettes.add(Arrays.hashCode(mask)),
            family + " duplicates another mineral silhouette"));

        assertTrue(centerCount(masks.get("hotstone")) >= 4, "Hotstone needs a compact impact core");
        assertTrue(maxRow(masks.get("coal_measures")) >= 9, "Coal Measures needs broad seams");
        assertTrue(maxColumn(masks.get("ironstone")) >= 7, "Ironstone needs thick brace uprights");
        assertTrue(maxRow(masks.get("evaporite_beds")) >= 9, "Evaporite needs a crystal bed");
    }

    private static int centerCount(boolean[] mask) {
        int count = 0;
        for (int y = 6; y <= 9; y++) for (int x = 6; x <= 9; x++) if (mask[y * 16 + x]) count++;
        return count;
    }

    private static int maxRow(boolean[] mask) {
        int best = 0;
        for (int y = 0; y < 16; y++) {
            int count = 0;
            for (int x = 0; x < 16; x++) if (mask[y * 16 + x]) count++;
            best = Math.max(best, count);
        }
        return best;
    }

    private static int maxColumn(boolean[] mask) {
        int best = 0;
        for (int x = 0; x < 16; x++) {
            int count = 0;
            for (int y = 0; y < 16; y++) if (mask[y * 16 + x]) count++;
            best = Math.max(best, count);
        }
        return best;
    }
}
