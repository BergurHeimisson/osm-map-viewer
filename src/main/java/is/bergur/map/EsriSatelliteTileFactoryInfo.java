package is.bergur.map;

import org.jxmapviewer.viewer.TileFactoryInfo;

public class EsriSatelliteTileFactoryInfo extends TileFactoryInfo {

    private static final int MAX_ZOOM = 17;

    public EsriSatelliteTileFactoryInfo() {
        super(1, MAX_ZOOM - 2, MAX_ZOOM, 256, true, true,
              "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/",
              "x", "y", "zoom");
    }

    @Override
    public String getTileUrl(int x, int y, int zoom) {
        int z = MAX_ZOOM - zoom;
        // ESRI uses z/y/x order (row, col), unlike OSM's z/x/y
        return baseURL + z + "/" + y + "/" + x;
    }
}
