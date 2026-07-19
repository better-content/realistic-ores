package io.github.realisticores;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class RealisticOresModWiringTest {
    private static final Path MOD_SOURCE =
            Path.of("src/main/java/io/github/realisticores/RealisticOresMod.java");

    @Test
    void registersMissingMappingsOnTheForgeEventBus() throws IOException {
        String source = Files.readString(MOD_SOURCE);

        assertTrue(
                source.contains("MinecraftForge.EVENT_BUS.addListener(this::remapLegacySurfaceSamples)"),
                "MissingMappingsEvent must use the Forge event bus");
        assertFalse(
                source.contains("modBus.addListener(this::remapLegacySurfaceSamples)"),
                "MissingMappingsEvent is not an IModBusEvent and crashes when registered on the mod bus");
    }
}
