package is.bergur.map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EsriSatelliteTileFactoryInfoTest {

    private final EsriSatelliteTileFactoryInfo info = new EsriSatelliteTileFactoryInfo();

    @Test
    void urlUsesHttps() {
        String url = info.getTileUrl(100, 200, 5);
        assertTrue(url.startsWith("https://"), "must use HTTPS");
    }

    @Test
    void urlContainsEsriHost() {
        String url = info.getTileUrl(100, 200, 5);
        assertTrue(url.contains("arcgisonline.com"));
    }

    @Test
    void urlOrderIsZoomYX() {
        // ESRI format: .../tile/{z}/{y}/{x}  (note: y before x, unlike OSM)
        int x = 10, y = 20, jxZoom = 5;
        int expectedZ = 17 - jxZoom; // = 12
        String url = info.getTileUrl(x, y, jxZoom);
        String suffix = "/" + expectedZ + "/" + y + "/" + x;
        assertTrue(url.endsWith(suffix), "expected suffix " + suffix + " in " + url);
    }
}
