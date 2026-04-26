package is.bergur.map;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class NominatimGeocoder {

    public record GeoResult(double lat, double lon) {}

    private final HttpClient httpClient;

    public NominatimGeocoder() {
        this.httpClient = HttpClient.newHttpClient();
    }

    // Tries each query in order, returning the first hit. Adds a 1.1 s delay
    // between attempts to respect Nominatim's 1-request/second policy.
    public Optional<GeoResult> searchFirst(String... queries) throws Exception {
        for (int i = 0; i < queries.length; i++) {
            if (i > 0) Thread.sleep(1100);
            Optional<GeoResult> r = search(queries[i]);
            if (r.isPresent()) return r;
        }
        return Optional.empty();
    }

    public Optional<GeoResult> search(String address) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(buildUrl(address)))
            .header("User-Agent", "BergurOsmMapApp/1.0 bergur.heimisson@gmail.com")
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return parseResponse(response.body());
    }

    String buildUrl(String address) {
        String encoded = URLEncoder.encode(address, StandardCharsets.UTF_8);
        return "https://nominatim.openstreetmap.org/search?q=" + encoded + "&format=json&limit=1";
    }

    Optional<GeoResult> parseResponse(String json) {
        JSONArray arr = new JSONArray(json);
        if (arr.isEmpty()) return Optional.empty();
        JSONObject first = arr.getJSONObject(0);
        double lat = Double.parseDouble(first.getString("lat"));
        double lon = Double.parseDouble(first.getString("lon"));
        return Optional.of(new GeoResult(lat, lon));
    }
}
