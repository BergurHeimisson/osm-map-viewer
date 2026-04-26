package is.bergur.map;

import org.jxmapviewer.viewer.TileFactoryInfo;

public class OsmHttpsTileFactoryInfo extends TileFactoryInfo {

    private static final int MAX_ZOOM = 17;

    public OsmHttpsTileFactoryInfo() {
        super(1, MAX_ZOOM - 2, MAX_ZOOM, 256, true, true,
              "https://tile.openstreetmap.org/", "x", "y", "zoom");
    }

    @Override
    public String getTileUrl(int x, int y, int zoom) {
        int z = MAX_ZOOM - zoom;
        return baseURL + z + "/" + x + "/" + y + ".png";
    }
}
