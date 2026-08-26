package com.bettercontent.realisticores.ore;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

final class SaliencePresentationContractTest {
    @Test
    void oreTooltipsUseTheCanonicalEightCellBadgeFont() throws Exception {
        byte[] badge = resource("/assets/realistic_ores/textures/gui/aspect_badges.png").readAllBytes();
        assertEquals("b59717a5da26f633cd120f09b750c15875577ec9c74a8c7838c32aea8ee5eeed",
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(badge)));
        var image = ImageIO.read(new java.io.ByteArrayInputStream(badge));
        assertEquals(144, image.getWidth()); assertEquals(18, image.getHeight());
        String font = new String(resource("/assets/realistic_ores/font/aspects.json").readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(font.contains(""));
    }

    private InputStream resource(String path) {
        InputStream stream = getClass().getResourceAsStream(path);
        assertNotNull(stream, path);
        return stream;
    }
}
