package is.bergur.map;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TileOverlayPainterTest {

    @Test
    void buildTileUrl_replacesAllPlaceholders() {
        String template = "https://tile.waymarkedtrails.org/cycling/{z}/{x}/{y}.png";
        assertEquals(
            "https://tile.waymarkedtrails.org/cycling/5/10/15.png",
            TileOverlayPainter.buildTileUrl(template, 10, 15, 5)
        );
    }

    @Test
    void latLonToTileXY_reykjavik_zoom1() {
        // Reykjavik (64.1355°N, 21.8954°W) at OSM zoom 1 → tile (0, 0)
        // fractional x ≈ 0.878, fractional y ≈ 0.535
        double[] xy = TileOverlayPainter.latLonToTileXY(64.1355, -21.8954, 1);
        assertEquals(0, (int) xy[0], "tile x should be 0");
        assertEquals(0, (int) xy[1], "tile y should be 0");
        assertTrue(xy[0] > 0.8 && xy[0] < 0.9, "fractional x ≈ 0.878, got " + xy[0]);
        assertTrue(xy[1] > 0.5 && xy[1] < 0.6, "fractional y ≈ 0.535, got " + xy[1]);
    }

    @Test
    void latLonToTileXY_greenwich_equator_zoom1() {
        // (0°, 0°) is at tile position (1.0, 1.0) — exact boundary between 4 tiles
        double[] xy = TileOverlayPainter.latLonToTileXY(0.0, 0.0, 1);
        assertEquals(1.0, xy[0], 0.001);
        assertEquals(1.0, xy[1], 0.001);
    }

    @Test
    void setUrlTemplate_doesNotThrow() {
        TileOverlayPainter painter = new TileOverlayPainter();
        painter.setUrlTemplate("https://tile.waymarkedtrails.org/cycling/{z}/{x}/{y}.png");
        painter.setUrlTemplate(null);
    }
}
