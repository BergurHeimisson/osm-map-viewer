package is.bergur.map;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class NominatimGeocoderTest {

    private final NominatimGeocoder geocoder = new NominatimGeocoder();

    @Test
    void parseValidResponse() {
        String json = """
            [{"lat":"64.1234","lon":"-21.9876","display_name":"Úthlið 16, Reykjavík"}]
            """;
        Optional<NominatimGeocoder.GeoResult> result = geocoder.parseResponse(json);
        assertTrue(result.isPresent());
        assertEquals(64.1234, result.get().lat(), 1e-6);
        assertEquals(-21.9876, result.get().lon(), 1e-6);
    }

    @Test
    void parseEmptyResponseReturnsEmpty() {
        Optional<NominatimGeocoder.GeoResult> result = geocoder.parseResponse("[]");
        assertTrue(result.isEmpty());
    }

    @Test
    void parseIcelandicCharactersInAddress() {
        String json = """
            [{"lat":"64.5000","lon":"-21.3500","display_name":"Vatnsendahlíð, Skorradalur"}]
            """;
        Optional<NominatimGeocoder.GeoResult> result = geocoder.parseResponse(json);
        assertTrue(result.isPresent());
        assertEquals(64.5000, result.get().lat(), 1e-4);
    }

    @Test
    void buildUrlEncodesAddressAndIncludesRequiredParams() {
        String url = geocoder.buildUrl("Úthlið 16, Reykjavík, Iceland");
        assertTrue(url.startsWith("https://nominatim.openstreetmap.org/search?q="));
        assertTrue(url.contains("format=json"));
        assertTrue(url.contains("limit=1"));
        assertFalse(url.contains(" "), "spaces must be encoded");
    }
}
