package is.bergur.map;

public class GeoUtils {

    // JXMapViewer zoom: 1 = street level (tiny delta), 17 = world view (large delta)
    public static double panDelta(int zoom) {
        return 0.001 * Math.pow(2, zoom - 1);
    }
}
