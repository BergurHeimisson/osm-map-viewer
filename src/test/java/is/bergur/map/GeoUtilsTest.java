package is.bergur.map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeoUtilsTest {

    @Test
    void panDeltaAtZoom1IsSmallest() {
        assertEquals(0.001, GeoUtils.panDelta(1), 1e-10);
    }

    @Test
    void panDeltaDoublesWithEachZoomLevel() {
        assertEquals(GeoUtils.panDelta(1) * 2, GeoUtils.panDelta(2), 1e-10);
        assertEquals(GeoUtils.panDelta(2) * 2, GeoUtils.panDelta(3), 1e-10);
    }

    @Test
    void panDeltaAtZoom17IsLarge() {
        assertTrue(GeoUtils.panDelta(17) > 10.0);
    }
}
