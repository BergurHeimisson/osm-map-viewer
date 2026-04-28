package is.bergur.map;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OpenTopoMapTileFactoryInfoTest {

    private final OpenTopoMapTileFactoryInfo info = new OpenTopoMapTileFactoryInfo();

    @Test
    void urlUsesHttps() {
        assertTrue(info.getTileUrl(10, 20, 5).startsWith("https://"));
    }

    @Test
    void urlContainsOpenTopoMapHost() {
        assertTrue(info.getTileUrl(10, 20, 5).contains("opentopomap.org"));
    }

    @Test
    void urlOrderIsZXY() {
        int x = 10, y = 20, jxZoom = 5;
        int expectedZ = 17 - jxZoom; // = 12
        String url = info.getTileUrl(x, y, jxZoom);
        assertTrue(url.endsWith("/" + expectedZ + "/" + x + "/" + y + ".png"),
            "expected /{z}/{x}/{y}.png suffix in: " + url);
    }
}
