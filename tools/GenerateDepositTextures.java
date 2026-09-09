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

/** Deterministically renders host-rock-dominant deposit faces with family-specific silhouettes. */
public final class GenerateDepositTextures {
    private static final int SIZE = 16;
    private static final List<String> FACES = List.of("north", "east", "south", "west", "up", "down");
    private static final int[] STONE = rgb("686868", "747474", "7f7f7f", "8f8f8f");
    private static final int[] DEEPSLATE_SIDE = rgb("2f2f37", "3d3d43", "515151", "646464", "797979");
    private static final int[] DEEPSLATE_END = rgb("3d3d43", "4b4b50", "5a5a5a", "646464", "747474");
    private static final Pattern FAMILY = Pattern.compile(
        "\\\"([^\\\"]+)\\\"\\s*:\\s*\\{\\s*\\\"morphology\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"\\s*,\\s*\\\"palette\\\"\\s*:\\s*\\[([^]]+)]");
    private static final Pattern COLOR = Pattern.compile("#[0-9a-fA-F]{6}");

    private record Family(String id, String morphology, int[] palette) {}
    private record Point(int x, int y) {}
    private record Candidate(Point point, double score, int rgb) {}

    private GenerateDepositTextures() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !(args[0].equals("--write") || args[0].equals("--check"))) {
            throw new IllegalArgumentException("usage: --write | --check");
        }
        boolean write = args[0].equals("--write");
        Path root = Path.of("").toAbsolutePath();
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

    private static BufferedImage render(Path root, Family family, String host, int variant, int faceIndex) throws IOException {
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
                ? candidates(ImageIO.read(master.toFile()), family, variant, faceIndex)
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

    private static List<Candidate> candidates(BufferedImage atlas, Family family, int variant, int face) throws IOException {
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
                add(points, 6, 5, 7, 5, 9, 6, 10, 6, 5, 8, 6, 8, 8, 8, 9, 8,
                    7, 10, 8, 10, 10, 11, 11, 11,
                    7, 7, 8, 7, 7, 8, 9, 9,
                    4, 4, 5, 5, 11, 5, 10, 7, 4, 10, 6, 9, 10, 10, 12, 12);
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
                add(points, 2, 5, 3, 5, 4, 6, 5, 6, 6, 6, 8, 7, 9, 7, 10, 7, 11, 6, 12, 6, 13, 6,
                    3, 10, 4, 10, 5, 10, 6, 9, 7, 9, 9, 9, 10, 10, 11, 10, 12, 10, 13, 11);
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
