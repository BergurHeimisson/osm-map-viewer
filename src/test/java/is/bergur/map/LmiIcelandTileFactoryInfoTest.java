package is.bergur.map;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LmiIcelandTileFactoryInfoTest {

    private final LmiIcelandTileFactoryInfo info = new LmiIcelandTileFactoryInfo();

    @Test
    void urlUsesHttps() {
        assertTrue(info.getTileUrl(10, 20, 5).startsWith("https://"));
    }

    @Test
    void urlContainsLmiHost() {
        assertTrue(info.getTileUrl(10, 20, 5).contains("wmts.lmi.is"));
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
