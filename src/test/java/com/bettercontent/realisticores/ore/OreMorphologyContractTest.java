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
        "hotstone", "asymmetric_breccia_pipe_with_radial_fissures",
        "copper_bloom", "branching_stockwork_with_oxidation_halos",
        "tin_quartz", "steep_quartz_lodes_with_cassiterite_splays",
        "brassroot", "asymmetric_dendritic_fracture_vein",
        "coal_measures", "broken_uneven_stratiform_carbon_seams",
        "ironstone", "rusty_lenticular_oolitic_beds_and_pods",
        "evaporite_beds", "stacked_salt_gypsum_beds_and_crystalline_pockets",
        "black_shale", "dark_laminations_with_sparse_violet_stringers");

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
            assertTrue(maxWindow(mask, 3) <= 7, family + " regressed to a dense ore blob");
            masks.put(family, mask);
        }
        Set<Integer> silhouettes = new HashSet<>();
        masks.forEach((family, mask) -> assertTrue(silhouettes.add(Arrays.hashCode(mask)),
            family + " duplicates another mineral silhouette"));

        assertFalse(isHorizontallySymmetric(masks.get("hotstone")), "Hotstone must not regress to a centered star");
        assertFalse(isVerticallySymmetric(masks.get("hotstone")), "Hotstone must remain an asymmetric breccia body");
        assertTrue(maxRowSpan(masks.get("coal_measures")) >= 10, "Coal Measures needs broad broken seams");
        assertTrue(maxRowSpan(masks.get("ironstone")) >= 10, "Ironstone needs broad lenticular bedding");
        assertTrue(maxRowSpan(masks.get("evaporite_beds")) >= 10, "Evaporite needs a broad crystal bed");
    }

    private static int centerCount(boolean[] mask) {
        int count = 0;
        for (int y = 6; y <= 9; y++) for (int x = 6; x <= 9; x++) if (mask[y * 16 + x]) count++;
        return count;
    }

    private static boolean isHorizontallySymmetric(boolean[] mask) {
        for (int y = 0; y < 16; y++) for (int x = 0; x < 8; x++)
            if (mask[y * 16 + x] != mask[y * 16 + 15 - x]) return false;
        return true;
    }

    private static boolean isVerticallySymmetric(boolean[] mask) {
        for (int y = 0; y < 8; y++) for (int x = 0; x < 16; x++)
            if (mask[y * 16 + x] != mask[(15 - y) * 16 + x]) return false;
        return true;
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

    private static int maxRowSpan(boolean[] mask) {
        int best = 0;
        for (int y = 0; y < 16; y++) {
            int first = -1, last = -1;
            for (int x = 0; x < 16; x++) if (mask[y * 16 + x]) {
                if (first < 0) first = x;
                last = x;
            }
            if (first >= 0) best = Math.max(best, last - first + 1);
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

    private static int maxWindow(boolean[] mask, int size) {
        int best = 0;
        for (int y0 = 0; y0 <= 16 - size; y0++) for (int x0 = 0; x0 <= 16 - size; x0++) {
            int count = 0;
            for (int y = y0; y < y0 + size; y++) for (int x = x0; x < x0 + size; x++)
                if (mask[y * 16 + x]) count++;
            best = Math.max(best, count);
        }
        return best;
    }
}
