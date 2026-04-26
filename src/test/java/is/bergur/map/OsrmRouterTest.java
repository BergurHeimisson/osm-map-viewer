package is.bergur.map;

import org.jxmapviewer.viewer.GeoPosition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class OsrmRouterTest {

    private final OsrmRouter router = new OsrmRouter();
    private final GeoPosition reykjavik   = new GeoPosition(64.136, -21.905);
    private final GeoPosition skorradalur = new GeoPosition(64.500, -21.350);

    @Test
    void buildUrlHasCorrectFormat() {
        String url = router.buildUrl(reykjavik, skorradalur);
        assertTrue(url.startsWith("https://router.project-osrm.org/route/v1/driving/"));
        assertTrue(url.contains("overview=full"));
        assertTrue(url.contains("geometries=geojson"));
        // OSRM expects lon,lat order
        assertTrue(url.contains("-21.905"), "must contain from-longitude first");
        assertTrue(url.contains("64.136"),  "must contain from-latitude second");
        assertFalse(url.contains(" "),      "no spaces in URL");
    }

    @Test
    void parseValidOsrmResponse() {
        String json = """
            {
              "code": "Ok",
              "routes": [{
                "geometry": {
                  "type": "LineString",
                  "coordinates": [[-21.905, 64.136], [-21.500, 64.300], [-21.350, 64.500]]
                }
              }]
            }
            """;
        Optional<OsrmRouter.Route> result = router.parseResponse(json);
        assertTrue(result.isPresent());
        List<GeoPosition> pts = result.get().points();
        assertEquals(3, pts.size());
        assertEquals(64.136, pts.get(0).getLatitude(),  1e-5);
        assertEquals(-21.905, pts.get(0).getLongitude(), 1e-5);
        assertEquals(64.500, pts.get(2).getLatitude(),  1e-5);
    }

    @Test
    void parseErrorCodeReturnsEmpty() {
        String json = """
            {"code": "NoRoute", "message": "No route found"}
            """;
        Optional<OsrmRouter.Route> result = router.parseResponse(json);
        assertTrue(result.isEmpty());
    }

    @Test
    void parseEmptyRoutesArrayReturnsEmpty() {
        String json = """
            {"code": "Ok", "routes": []}
            """;
        Optional<OsrmRouter.Route> result = router.parseResponse(json);
        assertTrue(result.isEmpty());
    }
}
