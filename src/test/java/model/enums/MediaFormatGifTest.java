package model.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class MediaFormatGifTest {
    @Test
    void gifIsSelectableAsAnAnimatedVideoFormat() {
        assertSame(MediaFormat.GIF, MediaFormat.fromButtonId("btnToGIF").orElseThrow());
        assertSame(MediaFormat.GIF, MediaFormat.fromExtension(".gif").orElseThrow());
        assertEquals(MediaType.VIDEO, MediaFormat.GIF.getMediaType());
        assertEquals("image/gif", MediaFormat.GIF.getMimeType());
    }
}
