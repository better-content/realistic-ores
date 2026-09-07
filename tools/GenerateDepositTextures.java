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

    private GenerateDepositTextures() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !(args[0].equals("--write") || args[0].equals("--check"))) {
            throw new IllegalArgumentException("usage: --write | --check");
        }
        boolean write = args[0].equals("--write");
        Path root = Path.of("").toAbsolutePath();
        Path output = root.resolve("src/main/resources/assets/realistic_ores/textures/block");
        for (Family family : families(root.resolve("tools/ore_art_manifest.json"))) {
            for (String host : List.of("stone", "deepslate")) {
                for (int variant = 0; variant < 3; variant++) {
                    for (int faceIndex = 0; faceIndex < FACES.size(); faceIndex++) {
                        String face = FACES.get(faceIndex);
                        BufferedImage expected = render(family, host, variant, faceIndex);
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

    private static BufferedImage render(Family family, String host, int variant, int faceIndex) {
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

        List<Point> points = transformed(mask(family.id()), variant, faceIndex);
        for (Point point : points) {
            int distance = Math.abs(point.x() - 8) + Math.abs(point.y() - 8);
            int colorIndex = Math.max(0, Math.min(4, 4 - distance / 2));
            image.setRGB(point.x(), point.y(), 0xff000000 | family.palette()[colorIndex]);
        }
        return image;
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
                add(points, 7, 7, 8, 7, 7, 8, 8, 8);
                for (int d = 2; d <= 4; d++) add(points, 8 - d, 8, 7 + d, 8, 8, 8 - d, 8, 7 + d);
                for (int d = 2; d <= 3; d++) add(points, 8 - d, 8 - d, 7 + d, 8 - d, 8 - d, 7 + d, 7 + d, 7 + d);
            }
            case "copper_bloom" -> {
                blob(points, 5, 6); blob(points, 9, 6); blob(points, 7, 10);
                add(points, 7, 7, 7, 8, 7, 9);
            }
            case "tin_quartz" -> {
                for (int x = 3; x <= 11; x++) add(points, x, 4 + (x - 3) / 3);
                for (int x = 4; x <= 12; x++) add(points, x, 9 + (x - 4) / 4);
                add(points, 5, 8, 6, 8, 9, 7, 10, 7, 11, 12, 12, 12);
            }
            case "brassroot" -> {
                for (int y = 5; y <= 12; y++) add(points, 7, y, 8, y);
                add(points, 6, 7, 5, 6, 4, 5, 3, 4, 9, 8, 10, 7, 11, 6, 12, 5,
                    6, 11, 5, 12, 9, 11, 10, 12);
            }
            case "coal_measures" -> {
                for (int x = 2; x <= 13; x++) if (x != 7) add(points, x, 5);
                for (int x = 3; x <= 13; x++) if (x != 9) add(points, x, 10);
                add(points, 4, 6, 5, 6, 11, 9, 12, 9);
            }
            case "ironstone" -> {
                for (int y = 4; y <= 11; y++) add(points, 4, y, 11, y);
                for (int x = 5; x <= 10; x++) add(points, x, 7, x, 8);
            }
            case "evaporite_beds" -> {
                for (int x = 3; x <= 11; x++) add(points, x, 12);
                for (int y = 8; y <= 11; y++) add(points, 4, y);
                for (int y = 4; y <= 11; y++) add(points, 7, y);
                for (int y = 9; y <= 11; y++) add(points, 10, y);
                add(points, 7, 3, 4, 7, 10, 8);
            }
            case "black_shale" -> {
                for (int d = 0; d <= 5; d++) add(points, 3 + d, 3 + d, 12 - d, 3 + d);
                add(points, 8, 9, 7, 10, 7, 11, 6, 12, 5, 13, 9, 9, 10, 10, 11, 10);
            }
            default -> throw new IllegalArgumentException("unknown morphology family " + id);
        }
        return points;
    }

    private static void blob(List<Point> points, int cx, int cy) {
        add(points, cx, cy, cx - 1, cy, cx + 1, cy, cx, cy - 1, cx, cy + 1, cx + 1, cy + 1);
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
