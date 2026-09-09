import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/** Renders host-rock-dominant pixel art from generated geological alpha geometry. */
public final class GenerateDepositTextures {
    private static final int SIZE = 16;
    private static final List<String> FACES = List.of("north", "east", "south", "west", "up", "down");
    private static final int[] STONE = rgb("686868", "747474", "7f7f7f", "8f8f8f");
    private static final int[] DEEPSLATE_SIDE = rgb("2f2f37", "3d3d43", "515151", "646464", "797979");
    private static final int[] DEEPSLATE_END = rgb("3d3d43", "4b4b50", "5a5a5a", "646464", "747474");
    private static final Map<String, int[]> PIXEL_ART_PALETTES = Map.of(
            "coal_measures", rgb("151719", "252a2e", "3b4248", "687078", "9a7a3f"),
            "ironstone", rgb("3b251c", "6b3d24", "96572d", "bd783b", "dda263"),
            "copper_bloom", rgb("3d3730", "817862", "b7aa8b", "a87835", "3f9873"),
            "tin_quartz", rgb("302d2a", "635b52", "a99e8c", "ded3bc", "f3ead9"),
            "brassroot", rgb("3a3026", "574a39", "716148", "8a7554", "a68c62"),
            "evaporite_beds", rgb("465057", "697880", "99abb0", "c2d3d3", "e5ece7"),
            "hotstone", rgb("402c2b", "6f6762", "7b3430", "b84a32", "e87a35"),
            "black_shale", rgb("17151a", "2d2932", "48404d", "75657b", "a58dac"));
    private static final Pattern FAMILY = Pattern.compile(
        "\\\"([^\\\"]+)\\\"\\s*:\\s*\\{\\s*\\\"morphology\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"\\s*,\\s*\\\"palette\\\"\\s*:\\s*\\[([^]]+)]");
    private static final Pattern COLOR = Pattern.compile("#[0-9a-fA-F]{6}");

    private record Family(String id, String morphology, int[] palette) {}
    private record Point(int x, int y) {}
    private record Candidate(Point point, double score, int rgb) {}

    private GenerateDepositTextures() {}

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        if (args.length == 2 && args[0].equals("--validate-candidates")) {
            validateCandidates(root, Path.of(args[1]));
            return;
        }
        if ((args.length == 5 || args.length == 6) && args[0].equals("--preview")) {
            int outputSize = args.length == 6 ? Integer.parseInt(args[5]) : 32;
            preview(root, args[1], Integer.parseInt(args[2]), Path.of(args[3]), Path.of(args[4]), outputSize);
            return;
        }
        if (args.length != 1 || !(args[0].equals("--write") || args[0].equals("--check"))) {
            throw new IllegalArgumentException(
                    "usage: --write | --check | --preview FAMILY VARIANT MASTER OUTPUT_DIRECTORY [16|32|64]"
                            + " | --validate-candidates DIRECTORY");
        }
        boolean write = args[0].equals("--write");
        Path output = root.resolve("src/main/resources/assets/realistic_ores/textures/block");
        for (Family family : families(root.resolve("tools/ore_art_manifest.json"))) {
            if (write) normalizeMasters(root, family);
            for (String host : List.of("stone", "deepslate")) {
                for (int variant = 0; variant < 3; variant++) {
                    for (int faceIndex = 0; faceIndex < FACES.size(); faceIndex++) {
                        String face = FACES.get(faceIndex);
                        BufferedImage expected = render(root, family, host, variant, faceIndex);
                        String prefix = host.equals("stone") ? "" : "deepslate_";
                        Path path = output.resolve(prefix + family.id() + "_" + variant + "_" + face + ".png");
                        if (write) ImageIO.write(expected, "png", path.toFile());
                        else assertEqual(expected, ImageIO.read(path.toFile()), path);
                    }
                }
            }
        }
        System.out.println((write ? "wrote" : "verified") + " 288 morphology-distinct deposit faces");
    }

    private static void preview(
            Path root, String familyId, int variant, Path master, Path output, int outputSize
    ) throws IOException {
        if (variant < 0 || variant > 2) throw new IOException("variant must be 0, 1, or 2");
        if (outputSize != 16 && outputSize != 32 && outputSize != 64)
            throw new IOException("preview size must be 16, 32, or 64");
        Family family = families(root.resolve("tools/ore_art_manifest.json")).stream()
                .filter(candidate -> candidate.id().equals(familyId)).findFirst()
                .orElseThrow(() -> new IOException("unknown family " + familyId));
        BufferedImage atlas = validateAlphaMaster(master);
        Files.createDirectories(output);
        for (String host : List.of("stone", "deepslate")) {
            for (int faceIndex = 0; faceIndex < FACES.size(); faceIndex++) {
                BufferedImage image = renderAlphaPreview(family, host, variant, faceIndex, atlas, outputSize);
                String prefix = host.equals("stone") ? "" : "deepslate_";
                Path path = output.resolve(prefix + family.id() + "_" + variant + "_" + FACES.get(faceIndex) + ".png");
                ImageIO.write(image, "png", path.toFile());
                System.out.println(path + " mineral_pixels="
                        + alphaCandidates(atlas, family, faceIndex, outputSize).size());
            }
        }
    }

    private static void validateCandidates(Path root, Path directory) throws IOException {
        int masters = 0;
        for (Family family : families(root.resolve("tools/ore_art_manifest.json"))) {
            Path familyDirectory = directory.resolve(family.id());
            if (!Files.isDirectory(familyDirectory))
                throw new IOException("missing candidate family directory " + familyDirectory);
            int familyMasters = 0;
            try (var paths = Files.list(familyDirectory)) {
                for (Path path : paths.filter(candidate -> candidate.getFileName().toString()
                                .matches("variant_[0-2]\\.png"))
                        .sorted().toList()) {
                    BufferedImage atlas = validateAlphaMaster(path);
                    for (int face = 0; face < FACES.size(); face++)
                        alphaCandidates(atlas, family, face, 32);
                    masters++;
                    familyMasters++;
                }
            }
            if (familyMasters != 3)
                throw new IOException(family.id() + " must have exactly three candidate masters, found "
                        + familyMasters);
        }
        if (masters != 24) throw new IOException("expected 24 candidate masters, found " + masters);
        System.out.println("validated " + masters + " alpha-source cubemap candidates at 32x32");
    }

    private static BufferedImage renderAlphaPreview(
            Family family, String host, int variant, int faceIndex, BufferedImage atlas, int size
    ) throws IOException {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        int[] hostPalette = host.equals("stone") ? STONE
                : faceIndex >= 4 ? DEEPSLATE_END : DEEPSLATE_SIDE;
        long seed = mix(family.id().hashCode() * 31L + host.hashCode() * 17L + variant * 7L + faceIndex);
        for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) {
            int hostX = x * SIZE / size;
            int hostY = y * SIZE / size;
            long noise = mix(seed + hostX * 0x9e3779b97f4a7c15L + hostY * 0xc2b2ae3d27d4eb4fL
                    + (hostX / 3) * 97L + (hostY / 3) * 193L);
            int value = Math.floorMod((int) (noise ^ noise >>> 32), hostPalette.length);
            image.setRGB(x, y, 0xff000000 | hostPalette[value]);
        }
        for (Candidate candidate : alphaCandidates(atlas, family, faceIndex, size)) {
            Point point = candidate.point();
            int mineralRgb = nearest(candidate.rgb(), PIXEL_ART_PALETTES.get(family.id()));
            image.setRGB(point.x(), point.y(), 0xff000000 | mineralRgb);
        }
        return image;
    }

    private static BufferedImage render(
            Path root, Family family, String host, int variant, int faceIndex
    ) throws IOException {
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        int[] hostPalette = host.equals("stone") ? STONE
            : faceIndex >= 4 ? DEEPSLATE_END : DEEPSLATE_SIDE;
        long seed = mix(family.id().hashCode() * 31L + host.hashCode() * 17L + variant * 7L + faceIndex);
        for (int y = 0; y < SIZE; y++) for (int x = 0; x < SIZE; x++) {
            long noise = mix(seed + x * 0x9e3779b97f4a7c15L + y * 0xc2b2ae3d27d4eb4fL
                + (x / 3) * 97L + (y / 3) * 193L);
            int value = Math.floorMod((int) (noise ^ noise >>> 32), hostPalette.length);
            image.setRGB(x, y, 0xff000000 | hostPalette[value]);
        }

        Path master = master(root, family.id(), variant);
        List<Candidate> candidates = Files.isRegularFile(master)
                ? maskedCandidates(ImageIO.read(master.toFile()), family, variant, faceIndex)
                : fallbackCandidates(family, variant, faceIndex);
        for (Candidate candidate : candidates) {
            Point point = candidate.point();
            int distance = Math.abs(point.x() - 8) + Math.abs(point.y() - 8);
            int color = candidate.rgb() == 0 ? family.palette()[Math.max(0, Math.min(4, 4 - distance / 2))]
                    : nearest(candidate.rgb(), family.palette());
            image.setRGB(point.x(), point.y(), 0xff000000 | color);
        }
        return image;
    }

    private static List<Candidate> fallbackCandidates(Family family, int variant, int faceIndex) {
        return transformed(mask(family.id()), variant, faceIndex).stream()
                .map(point -> new Candidate(point, 1.0, 0)).toList();
    }

    private static List<Candidate> maskedCandidates(BufferedImage atlas, Family family, int variant, int face) throws IOException {
        if (atlas == null || atlas.getWidth() % 3 != 0 || atlas.getHeight() % 2 != 0)
            throw new IOException("master must be a 3x2 atlas: " + family.id() + " variant " + variant);
        int cellWidth = atlas.getWidth() / 3;
        int cellHeight = atlas.getHeight() / 2;
        int cellX = face % 3;
        int cellY = face / 3;
        List<Candidate> result = new ArrayList<>();
        for (Point point : transformed(mask(family.id()), variant, face)) {
            int x = point.x(), y = point.y();
            int x0 = cellX * cellWidth + x * cellWidth / SIZE;
            int x1 = cellX * cellWidth + (x + 1) * cellWidth / SIZE;
            int y0 = cellY * cellHeight + y * cellHeight / SIZE;
            int y1 = cellY * cellHeight + (y + 1) * cellHeight / SIZE;
            double score = 0; double red = 0; double green = 0; double blue = 0;
            for (int sy = y0; sy < y1; sy += 2) for (int sx = x0; sx < x1; sx += 2) {
                int argb = atlas.getRGB(sx, sy);
                double weight = (argb >>> 24) / 255.0;
                score += weight; red += ((argb >>> 16) & 255) * weight;
                green += ((argb >>> 8) & 255) * weight; blue += (argb & 255) * weight;
            }
            int samples = Math.max(1, ((y1 - y0 + 1) / 2) * ((x1 - x0 + 1) / 2));
            score /= samples;
            int rgb = score <= 0 ? family.palette()[2] : ((int) (red / (score * samples)) << 16)
                    | ((int) (green / (score * samples)) << 8) | (int) (blue / (score * samples));
            result.add(new Candidate(point, score, rgb));
        }
        return result;
    }

    private static List<Candidate> alphaCandidates(
            BufferedImage atlas, Family family, int face, int outputSize
    ) throws IOException {
        if (atlas == null || atlas.getWidth() % 3 != 0 || atlas.getHeight() % 2 != 0)
            throw new IOException("master must be a 3x2 atlas: " + family.id());
        int cellWidth = atlas.getWidth() / 3;
        int cellHeight = atlas.getHeight() / 2;
        int cellX = face % 3;
        int cellY = face / 3;
        List<Candidate> result = new ArrayList<>();
        for (int y = 0; y < outputSize; y++) for (int x = 0; x < outputSize; x++) {
            int x0 = cellX * cellWidth + x * cellWidth / outputSize;
            int x1 = cellX * cellWidth + (x + 1) * cellWidth / outputSize;
            int y0 = cellY * cellHeight + y * cellHeight / outputSize;
            int y1 = cellY * cellHeight + (y + 1) * cellHeight / outputSize;
            double alpha = 0, red = 0, green = 0, blue = 0;
            int samples = 0, strong = 0, peak = 0;
            int accentScore = -1, accentRgb = 0;
            for (int sy = y0; sy < y1; sy++) for (int sx = x0; sx < x1; sx++) {
                int argb = atlas.getRGB(sx, sy);
                int sampleAlpha = argb >>> 24;
                double weight = sampleAlpha / 255.0;
                int sampleRed = (argb >>> 16) & 255;
                int sampleGreen = (argb >>> 8) & 255;
                int sampleBlue = argb & 255;
                alpha += weight;
                red += sampleRed * weight;
                green += sampleGreen * weight;
                blue += sampleBlue * weight;
                int maximumChannel = Math.max(sampleRed, Math.max(sampleGreen, sampleBlue));
                int chroma = maximumChannel - Math.min(sampleRed, Math.min(sampleGreen, sampleBlue));
                int sampleAccentScore = chroma + maximumChannel / 4;
                boolean accent = switch (family.id()) {
                    case "hotstone" -> maximumChannel >= 235 && chroma >= 80;
                    case "coal_measures" -> maximumChannel >= 120 && chroma >= 50;
                    case "copper_bloom" -> chroma >= 45;
                    default -> chroma >= 32;
                };
                if (sampleAlpha >= 128 && accent && sampleAccentScore > accentScore) {
                    accentScore = sampleAccentScore;
                    accentRgb = sampleRed << 16 | sampleGreen << 8 | sampleBlue;
                }
                if (sampleAlpha >= 128) strong++;
                peak = Math.max(peak, sampleAlpha);
                samples++;
            }
            double coverage = alpha / samples;
            double strongCoverage = strong / (double) samples;
            double coverageThreshold = coverageThreshold(family.id(), outputSize);
            double strongThreshold = outputSize <= 32 ? coverageThreshold / 3.0 : 0.010;
            if (coverage < coverageThreshold && !(peak >= 224 && strongCoverage >= strongThreshold)) continue;
            int rgb = accentScore >= 48 ? accentRgb : alpha <= 0 ? family.palette()[2]
                    : ((int) Math.round(red / alpha) << 16)
                    | ((int) Math.round(green / alpha) << 8) | (int) Math.round(blue / alpha);
            result.add(new Candidate(new Point(x, y), coverage, rgb));
        }
        boolean narrowEndSection = face >= 4
                && (family.id().equals("tin_quartz") || family.id().equals("brassroot")
                || family.id().equals("evaporite_beds") || family.id().equals("ironstone"));
        int minimumPercent = narrowEndSection ? 2 : 4;
        int minimum = Math.max(20, outputSize * outputSize * minimumPercent / 100);
        int maximumPercent = maximumCoveragePercent(family.id(), outputSize);
        int maximum = outputSize * outputSize * maximumPercent / 100;
        if (result.size() < minimum || result.size() > maximum)
            throw new IOException(family.id() + " face " + FACES.get(face)
                    + " reduces to " + result.size() + " mineral pixels; expected " + minimum + ".." + maximum);
        return result;
    }

    private static double coverageThreshold(String family, int outputSize) {
        if (outputSize <= 16) return switch (family) {
            case "hotstone" -> 0.350;
            case "copper_bloom" -> 0.180;
            default -> 0.100;
        };
        if (outputSize <= 32) return switch (family) {
            case "hotstone" -> 0.240;
            case "copper_bloom" -> 0.060;
            case "ironstone" -> 0.100;
            default -> 0.050;
        };
        return 0.035;
    }

    private static int maximumCoveragePercent(String family, int outputSize) {
        if (outputSize <= 16 && (family.equals("copper_bloom") || family.equals("hotstone"))) return 45;
        if (family.equals("ironstone")) return 38;
        if (family.equals("copper_bloom")) return 31;
        if (family.equals("tin_quartz")) return 34;
        return family.equals("hotstone") ? 36 : 30;
    }

    private static BufferedImage validateAlphaMaster(Path path) throws IOException {
        BufferedImage source = ImageIO.read(path.toFile());
        if (source == null) throw new IOException("invalid master " + path);
        if (source.getWidth() != 1536 || source.getHeight() != 1024)
            throw new IOException("master must be exactly 1536x1024: " + path);
        if (!source.getColorModel().hasAlpha()) throw new IOException("master lacks alpha: " + path);
        long transparent = 0, borderOpaque = 0, borderPixels = 0;
        long[] cellTransparent = new long[6];
        for (int y = 0; y < source.getHeight(); y++) for (int x = 0; x < source.getWidth(); x++) {
            int alpha = source.getRGB(x, y) >>> 24;
            if (alpha <= 16) {
                transparent++;
                cellTransparent[(y / 512) * 3 + x / 512]++;
            }
            if (x < 8 || y < 8 || x >= source.getWidth() - 8 || y >= source.getHeight() - 8) {
                borderPixels++;
                if (alpha >= 32) borderOpaque++;
            }
        }
        long pixels = (long) source.getWidth() * source.getHeight();
        if (transparent < pixels * 65 / 100)
            throw new IOException("master must be at least 65% transparent: " + path);
        for (int cell = 0; cell < cellTransparent.length; cell++)
            if (cellTransparent[cell] < 512L * 512L * 60L / 100L)
                throw new IOException("master cell " + FACES.get(cell)
                        + " must be at least 60% transparent: " + path);
        // Cubemap features must be allowed to cross atlas edges. A matte still makes nearly the
        // entire perimeter opaque, while real seam/vein intersections occupy isolated segments.
        if (borderOpaque > borderPixels / 2)
            throw new IOException("master has an opaque perimeter consistent with a matte: " + path);
        return source;
    }

    private static void normalizeMasters(Path root, Family family) throws IOException {
        for (int variant = 0; variant < 3; variant++) {
            Path path = master(root, family.id(), variant);
            if (!Files.isRegularFile(path)) continue;
            BufferedImage source = ImageIO.read(path.toFile());
            if (source == null) throw new IOException("invalid master " + path);
            boolean meaningfulAlpha = false;
            for (int y = 0; y < source.getHeight() && !meaningfulAlpha; y += 8)
                for (int x = 0; x < source.getWidth(); x += 8)
                    if ((source.getRGB(x, y) >>> 24) < 240) { meaningfulAlpha = true; break; }
            BufferedImage normalized = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < source.getHeight(); y++) for (int x = 0; x < source.getWidth(); x++) {
                int argb = source.getRGB(x, y); int alpha = argb >>> 24;
                int r = (argb >>> 16) & 255, g = (argb >>> 8) & 255, b = argb & 255;
                int max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
                boolean foreground = meaningfulAlpha ? alpha >= 48 : (max < 115 || max - min > 28);
                normalized.setRGB(x, y, foreground ? (0xff000000 | (argb & 0xffffff)) : 0);
            }
            ImageIO.write(normalized, "png", path.toFile());
        }
    }

    private static Path master(Path root, String family, int variant) {
        return root.resolve("art/block-masters").resolve(family).resolve("variant_" + variant + ".png");
    }

    private static int nearest(int rgb, int[] palette) {
        int best = palette[0], bestDistance = Integer.MAX_VALUE;
        for (int candidate : palette) {
            int dr = ((rgb >>> 16) & 255) - ((candidate >>> 16) & 255);
            int dg = ((rgb >>> 8) & 255) - ((candidate >>> 8) & 255);
            int db = (rgb & 255) - (candidate & 255);
            int distance = dr * dr + dg * dg + db * db;
            if (distance < bestDistance) { bestDistance = distance; best = candidate; }
        }
        return best;
    }

    private static List<Point> transformed(List<Point> source, int variant, int face) {
        Map<Integer, Point> unique = new LinkedHashMap<>();
        boolean mirror = ((variant + face) & 1) == 1;
        int dx = Math.floorMod(face * 5 + variant * 3, 3) - 1;
        int dy = Math.floorMod(face * 3 + variant * 5, 3) - 1;
        for (Point point : source) {
            int x = mirror ? 15 - point.x() : point.x();
            int y = point.y();
            x = Math.max(1, Math.min(14, x + dx));
            y = Math.max(1, Math.min(14, y + dy));
            unique.put(y * SIZE + x, new Point(x, y));
        }
        return List.copyOf(unique.values());
    }

    private static List<Point> mask(String id) {
        List<Point> points = new ArrayList<>();
        switch (id) {
            case "hotstone" -> {
                add(points, 4, 4, 5, 4, 4, 5, 9, 4, 9, 5, 10, 5,
                    6, 8, 7, 8, 6, 9, 10, 9, 11, 9, 11, 10,
                    5, 12, 6, 11, 6, 12,
                    3, 7, 4, 7, 5, 6, 7, 7, 8, 7, 9, 8, 12, 11, 13, 12);
            }
            case "copper_bloom" -> {
                add(points, 3, 11, 4, 10, 5, 9, 6, 8, 7, 7, 8, 6, 9, 5, 10, 4, 11, 3,
                    5, 4, 6, 5, 7, 6, 8, 7, 9, 8, 10, 9, 11, 10,
                    4, 11, 5, 10, 8, 5, 9, 4, 10, 8, 11, 9);
            }
            case "tin_quartz" -> {
                add(points, 4, 12, 5, 11, 5, 10, 6, 9, 6, 8, 7, 7, 7, 6, 8, 5, 8, 4, 9, 3,
                    6, 10, 7, 10, 8, 9, 9, 8, 10, 7, 11, 6,
                    4, 11, 8, 8, 9, 6, 10, 5, 11, 4);
            }
            case "brassroot" -> {
                add(points, 7, 3, 7, 4, 7, 5, 8, 6, 8, 7, 8, 8, 9, 9, 9, 10, 9, 11, 10, 12,
                    6, 6, 5, 7, 4, 8, 3, 9, 9, 8, 10, 7, 11, 6, 12, 5,
                    8, 10, 7, 11, 6, 12);
            }
            case "coal_measures" -> {
                add(points, 2, 5, 3, 5, 4, 4, 5, 4, 6, 4, 8, 5, 9, 5, 10, 5, 11, 4, 12, 4, 13, 4,
                    3, 10, 4, 10, 5, 10, 6, 11, 7, 11, 8, 11, 10, 10, 11, 10, 12, 10, 13, 10);
            }
            case "ironstone" -> {
                add(points, 2, 6, 3, 6, 4, 6, 5, 7, 6, 7, 7, 7, 8, 7, 9, 6, 10, 6, 11, 6, 12, 6, 13, 6,
                    3, 9, 4, 9, 5, 9, 6, 10, 7, 10, 8, 10, 9, 10, 10, 9, 11, 9, 12, 9,
                    5, 5, 8, 8, 11, 10);
            }
            case "evaporite_beds" -> {
                add(points, 2, 5, 3, 5, 4, 5, 5, 5, 7, 5, 8, 5, 9, 5, 10, 5, 11, 5, 12, 5, 13, 5,
                    3, 9, 4, 9, 5, 9, 6, 9, 7, 9, 8, 9, 10, 9, 11, 9, 12, 9, 13, 9,
                    5, 8, 6, 7, 7, 8, 10, 8, 11, 7, 12, 8);
            }
            case "black_shale" -> {
                add(points, 2, 4, 3, 4, 4, 5, 5, 5, 6, 5, 7, 6, 8, 6, 9, 6, 10, 7, 11, 7, 12, 7, 13, 8,
                    2, 10, 3, 10, 4, 10, 5, 11, 6, 11, 8, 12, 9, 12, 10, 12, 11, 13, 12, 13,
                    6, 8, 7, 9, 8, 10);
            }
            default -> throw new IllegalArgumentException("unknown morphology family " + id);
        }
        return points;
    }

    private static void add(List<Point> points, int... coordinates) {
        for (int index = 0; index < coordinates.length; index += 2)
            points.add(new Point(coordinates[index], coordinates[index + 1]));
    }

    private static List<Family> families(Path manifest) throws IOException {
        Matcher rows = FAMILY.matcher(Files.readString(manifest));
        List<Family> result = new ArrayList<>();
        while (rows.find()) {
            Matcher colors = COLOR.matcher(rows.group(3));
            List<Integer> palette = new ArrayList<>();
            while (colors.find()) palette.add(Integer.parseInt(colors.group().substring(1), 16));
            if (palette.size() != 5) throw new IOException("expected five colors for " + rows.group(1));
            result.add(new Family(rows.group(1), rows.group(2), palette.stream().mapToInt(Integer::intValue).toArray()));
        }
        if (result.size() != 8) throw new IOException("expected eight families, found " + result.size());
        return result;
    }

    private static void assertEqual(BufferedImage expected, BufferedImage actual, Path path) throws IOException {
        if (actual == null || actual.getWidth() != SIZE || actual.getHeight() != SIZE)
            throw new IOException("invalid texture " + path);
        for (int y = 0; y < SIZE; y++) for (int x = 0; x < SIZE; x++)
            if (expected.getRGB(x, y) != actual.getRGB(x, y))
                throw new IOException("generated texture differs at " + x + "," + y + ": " + path);
    }

    private static int[] rgb(String... values) {
        return java.util.Arrays.stream(values).mapToInt(value -> Integer.parseInt(value, 16)).toArray();
    }

    private static long mix(long value) {
        value ^= value >>> 33; value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33; value *= 0xc4ceb9fe1a85ec53L;
        return value ^ value >>> 33;
    }
}
