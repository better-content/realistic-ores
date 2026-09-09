package com.bettercontent.realisticores.worldgen;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;

/** Deterministic, host-independent geological shapes used by the configured ore feature. */
public enum DepositMorphology implements StringRepresentable {
    BROKEN_STRATIFORM_SEAM("broken_stratiform_seam"),
    LENTICULAR_OOLITIC_BED("lenticular_oolitic_bed"),
    BRANCHING_STOCKWORK("branching_stockwork"),
    STEEP_QUARTZ_LODE("steep_quartz_lode"),
    DENDRITIC_FRACTURE_VEIN("dendritic_fracture_vein"),
    STACKED_EVAPORITE_BEDS("stacked_evaporite_beds"),
    BRECCIA_PIPE_WITH_RADIAL_DIKES("breccia_pipe_with_radial_dikes"),
    LAMINATED_SHALE_STRINGERS("laminated_shale_stringers");

    public static final Codec<DepositMorphology> CODEC = StringRepresentable.fromEnum(DepositMorphology::values);

    private static final List<BlockPos> NEIGHBORS = List.of(
            new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0),
            new BlockPos(0, 1, 0), new BlockPos(0, -1, 0),
            new BlockPos(0, 0, 1), new BlockPos(0, 0, -1));

    private final String serializedName;

    DepositMorphology(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    public List<BlockPos> sample(RandomSource random, int blockBudget) {
        LinkedHashSet<BlockPos> points = new LinkedHashSet<>();
        points.add(BlockPos.ZERO);
        switch (this) {
            case BROKEN_STRATIFORM_SEAM -> brokenSeam(points, random, blockBudget);
            case LENTICULAR_OOLITIC_BED -> lenticularBed(points, random, blockBudget);
            case BRANCHING_STOCKWORK -> stockwork(points, random, blockBudget);
            case STEEP_QUARTZ_LODE -> quartzLode(points, random, blockBudget);
            case DENDRITIC_FRACTURE_VEIN -> dendriticVein(points, random, blockBudget);
            case STACKED_EVAPORITE_BEDS -> evaporiteBeds(points, random, blockBudget);
            case BRECCIA_PIPE_WITH_RADIAL_DIKES -> brecciaPipe(points, random, blockBudget);
            case LAMINATED_SHALE_STRINGERS -> shaleStringers(points, random, blockBudget);
        }
        return normalize(points, random, blockBudget);
    }

    private static void brokenSeam(Set<BlockPos> points, RandomSource random, int budget) {
        boolean alongX = random.nextBoolean();
        int length = Math.max(8, budget / 2);
        int drift = 0;
        for (int step = -length / 2; step <= length / 2; step++) {
            if (Math.floorMod(step, 4) == 0) drift += random.nextInt(3) - 1;
            if (random.nextFloat() < 0.14F) continue;
            int width = 1 + random.nextInt(2);
            for (int cross = -width; cross <= width; cross++) {
                add(points, alongX ? step : cross, drift, alongX ? cross : step);
            }
        }
    }

    private static void lenticularBed(Set<BlockPos> points, RandomSource random, int budget) {
        int lenses = Math.max(2, budget / 12);
        for (int lens = 0; lens < lenses; lens++) {
            int cx = (lens - lenses / 2) * 3 + random.nextInt(3) - 1;
            int cy = random.nextInt(3) - 1;
            int cz = random.nextInt(5) - 2;
            int radius = 2 + random.nextInt(2);
            for (int x = -radius; x <= radius; x++) {
                for (int z = -2; z <= 2; z++) {
                    if (x * x * 2 + z * z * 3 <= radius * radius * 2 && random.nextFloat() > 0.12F) {
                        add(points, cx + x, cy, cz + z);
                        if (random.nextFloat() < 0.24F) add(points, cx + x, cy + 1, cz + z);
                    }
                }
            }
        }
    }

    private static void stockwork(Set<BlockPos> points, RandomSource random, int budget) {
        add(points, 0, 0, 0);
        int branches = 4 + random.nextInt(3);
        for (int branch = 0; branch < branches; branch++) {
            BlockPos cursor = BlockPos.ZERO;
            int dx = random.nextBoolean() ? 1 : -1;
            int dy = random.nextInt(3) - 1;
            int dz = random.nextBoolean() ? 1 : -1;
            for (int step = 0; step < Math.max(4, budget / branches); step++) {
                cursor = cursor.offset(dx, dy, dz);
                points.add(cursor);
                if (random.nextFloat() < 0.35F) points.add(cursor.offset(0, random.nextBoolean() ? 1 : -1, 0));
                if (random.nextFloat() < 0.3F) dx = random.nextInt(3) - 1;
                if (random.nextFloat() < 0.3F) dy = random.nextInt(3) - 1;
                if (random.nextFloat() < 0.3F) dz = random.nextInt(3) - 1;
                if (dx == 0 && dy == 0 && dz == 0) dx = 1;
            }
        }
    }

    private static void quartzLode(Set<BlockPos> points, RandomSource random, int budget) {
        int length = Math.max(8, budget / 2);
        boolean alongX = random.nextBoolean();
        for (int step = -length / 2; step <= length / 2; step++) {
            int rise = Math.floorDiv(step + length / 2, 2) - length / 4;
            int sway = Math.floorMod(step, 5) == 0 ? random.nextInt(3) - 1 : 0;
            add(points, alongX ? step : sway, rise, alongX ? sway : step);
            add(points, alongX ? step : sway + 1, rise, alongX ? sway + 1 : step);
            if (random.nextFloat() < 0.32F) add(points, alongX ? step : sway - 1, rise + 1, alongX ? sway - 1 : step);
        }
    }

    private static void dendriticVein(Set<BlockPos> points, RandomSource random, int budget) {
        List<BlockPos> tips = new ArrayList<>();
        tips.add(BlockPos.ZERO);
        points.add(BlockPos.ZERO);
        while (points.size() < budget * 2 && !tips.isEmpty()) {
            BlockPos tip = tips.remove(random.nextInt(tips.size()));
            int children = random.nextFloat() < 0.42F ? 2 : 1;
            for (int child = 0; child < children; child++) {
                BlockPos cursor = tip;
                int dx = random.nextInt(3) - 1;
                int dy = random.nextBoolean() ? 1 : -1;
                int dz = random.nextInt(3) - 1;
                if (dx == 0 && dz == 0) dx = random.nextBoolean() ? 1 : -1;
                int length = 2 + random.nextInt(4);
                for (int step = 0; step < length; step++) {
                    cursor = cursor.offset(dx, dy, dz);
                    points.add(cursor);
                }
                if (cursor.distManhattan(BlockPos.ZERO) < 12) tips.add(cursor);
            }
        }
    }

    private static void evaporiteBeds(Set<BlockPos> points, RandomSource random, int budget) {
        int radius = Math.max(3, (int) Math.ceil(Math.sqrt(budget / 2.0D)));
        for (int layer : new int[] {-1, 1}) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + z * z <= radius * radius && random.nextFloat() > 0.22F) {
                        add(points, x, layer, z);
                    }
                }
            }
        }
        for (int x = -radius / 2; x <= radius / 2; x++) add(points, x, 0, 0);
    }

    private static void brecciaPipe(Set<BlockPos> points, RandomSource random, int budget) {
        int height = Math.max(7, budget / 3);
        for (int y = -height / 2; y <= height / 2; y++) {
            add(points, 0, y, 0);
            if (Math.floorMod(y, 2) == 0) {
                add(points, 1, y, 0, -1, y, 0, 0, y, 1, 0, y, -1);
            }
        }
        int spokes = 4 + random.nextInt(3);
        for (int spoke = 0; spoke < spokes; spoke++) {
            int dx = random.nextInt(3) - 1;
            int dz = random.nextInt(3) - 1;
            if (dx == 0 && dz == 0) dx = 1;
            int y = random.nextInt(Math.max(1, height)) - height / 2;
            BlockPos cursor = new BlockPos(0, y, 0);
            for (int step = 0; step < 2 + random.nextInt(4); step++) {
                cursor = cursor.offset(dx, random.nextInt(3) - 1, dz);
                points.add(cursor);
            }
        }
    }

    private static void shaleStringers(Set<BlockPos> points, RandomSource random, int budget) {
        int length = Math.max(8, budget / 2);
        for (int x = -length / 2; x <= length / 2; x++) {
            int y = (int) Math.round(Math.sin((x + 2) * 0.7D));
            if (random.nextFloat() > 0.18F) add(points, x, y, 0);
            if (random.nextFloat() < 0.55F) add(points, x, y, 1);
            if (Math.floorMod(x, 5) == 0) {
                int direction = random.nextBoolean() ? 1 : -1;
                add(points, x, y + direction, 0, x, y + direction * 2, 0);
            }
        }
    }

    private static List<BlockPos> normalize(LinkedHashSet<BlockPos> points, RandomSource random, int budget) {
        points.add(BlockPos.ZERO);
        while (points.size() < budget) {
            List<BlockPos> existing = new ArrayList<>(points);
            BlockPos base = existing.get(random.nextInt(existing.size()));
            points.add(base.offset(NEIGHBORS.get(random.nextInt(NEIGHBORS.size()))));
        }
        List<BlockPos> result = new ArrayList<>(points);
        if (result.size() > budget) result = new ArrayList<>(result.subList(0, budget));
        Collections.shuffle(result, new java.util.Random(random.nextLong()));
        return List.copyOf(result);
    }

    private static void add(Set<BlockPos> points, int... coordinates) {
        for (int index = 0; index < coordinates.length; index += 3) {
            points.add(new BlockPos(coordinates[index], coordinates[index + 1], coordinates[index + 2]));
        }
    }
}
