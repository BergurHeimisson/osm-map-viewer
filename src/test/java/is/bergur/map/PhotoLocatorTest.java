package is.bergur.map;

import org.jxmapviewer.viewer.GeoPosition;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PhotoLocatorTest {

    @Test
    void readsGpsFromJpegWithExif() throws Exception {
        File photo = new File(getClass().getResource("/is/bergur/map/gps_test.jpg").toURI());
        Optional<GeoPosition> result = PhotoLocator.readGps(photo);
        assertTrue(result.isPresent(), "Expected GPS position to be found");
        assertEquals(64.1355, result.get().getLatitude(),  0.001);
        assertEquals(-21.8954, result.get().getLongitude(), 0.001);
    }

    @Test
    void returnsEmptyForFileWithoutGps() throws Exception {
        File noGps = File.createTempFile("nogps", ".jpg");
        noGps.deleteOnExit();
        java.awt.image.BufferedImage img =
            new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_RGB);
        javax.imageio.ImageIO.write(img, "jpg", noGps);
        Optional<GeoPosition> result = PhotoLocator.readGps(noGps);
        assertFalse(result.isPresent(), "Expected no GPS for plain JPEG");
    }

    @Test
    void returnsEmptyForNonImageFile() throws Exception {
        File txt = File.createTempFile("test", ".txt");
        txt.deleteOnExit();
        Optional<GeoPosition> result = PhotoLocator.readGps(txt);
        assertFalse(result.isPresent(), "Expected no GPS for non-image file");
    }
}
