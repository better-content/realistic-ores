package com.bettercontent.realisticores.worldgen;

import com.mojang.serialization.JsonOps;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DepositMorphologyTest {
    @Test void serializedNamesRoundTripThroughTheCodec() {
        for (DepositMorphology morphology : DepositMorphology.values()) {
            var encoded = DepositMorphology.CODEC.encodeStart(JsonOps.INSTANCE, morphology).result().orElseThrow();
            assertEquals(morphology.getSerializedName(), encoded.getAsString());
            assertEquals(morphology, DepositMorphology.CODEC.parse(JsonOps.INSTANCE, encoded).result().orElseThrow());
        }
    }

    @Test void everyMorphologyIsDeterministicBoundedAndHonorsItsBudget() {
        Set<Set<BlockPos>> silhouettes = new HashSet<>();
        for (DepositMorphology morphology : DepositMorphology.values()) {
            List<BlockPos> first = morphology.sample(RandomSource.create(0x5eedL), 48);
            List<BlockPos> replay = morphology.sample(RandomSource.create(0x5eedL), 48);
            assertEquals(first, replay, morphology.getSerializedName());
            assertEquals(48, new HashSet<>(first).size(), morphology.getSerializedName());
            assertTrue(first.contains(BlockPos.ZERO), morphology.getSerializedName());
            assertTrue(silhouettes.add(Set.copyOf(first)), morphology.getSerializedName());
            assertTrue(first.stream().allMatch(pos -> Math.abs(pos.getX()) <= 32
                            && Math.abs(pos.getY()) <= 32 && Math.abs(pos.getZ()) <= 32),
                    morphology.getSerializedName());
        }
    }

    @Test void altitudeIdentityShapesRemainGeologicallyDistinct() {
        Set<BlockPos> coal = sample(DepositMorphology.BROKEN_STRATIFORM_SEAM);
        Set<BlockPos> tin = sample(DepositMorphology.STEEP_QUARTZ_LODE);
        Set<BlockPos> hot = sample(DepositMorphology.BRECCIA_PIPE_WITH_RADIAL_DIKES);
        assertTrue(Math.max(span(coal, 'x'), span(coal, 'z')) >= 2 * span(coal, 'y'),
                "coal should read as strata");
        assertTrue(span(tin, 'y') >= 6, "tin should climb as a steep lode");
        assertTrue(span(hot, 'y') >= span(hot, 'x'), "hotstone should retain a pipe axis");
        assertNotEquals(coal, tin);
        assertNotEquals(tin, hot);
    }

    private static Set<BlockPos> sample(DepositMorphology morphology) {
        return Set.copyOf(morphology.sample(RandomSource.create(981723L), 48));
    }

    private static int span(Set<BlockPos> points, char axis) {
        int min = points.stream().mapToInt(p -> coordinate(p, axis)).min().orElseThrow();
        int max = points.stream().mapToInt(p -> coordinate(p, axis)).max().orElseThrow();
        return max - min + 1;
    }

    private static int coordinate(BlockPos pos, char axis) {
        return switch (axis) {
            case 'x' -> pos.getX();
            case 'y' -> pos.getY();
            case 'z' -> pos.getZ();
            default -> throw new IllegalArgumentException("unknown axis " + axis);
        };
    }
}
