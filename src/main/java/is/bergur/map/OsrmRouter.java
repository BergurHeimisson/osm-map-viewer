package is.bergur.map;

import org.jxmapviewer.viewer.GeoPosition;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class OsrmRouter {

    public record Route(List<GeoPosition> points) {}

    private final HttpClient httpClient;

    public OsrmRouter() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public Optional<Route> fetchRoute(GeoPosition from, GeoPosition to) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(buildUrl(from, to)))
            .header("User-Agent", "BergurOsmMapApp/1.0 bergur.heimisson@gmail.com")
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return parseResponse(response.body());
    }

    String buildUrl(GeoPosition from, GeoPosition to) {
        return String.format(Locale.US,
            "https://router.project-osrm.org/route/v1/driving/%.6f,%.6f;%.6f,%.6f?overview=full&geometries=geojson",
            from.getLongitude(), from.getLatitude(),
            to.getLongitude(), to.getLatitude());
    }

    Optional<Route> parseResponse(String json) {
        JSONObject obj = new JSONObject(json);
        if (!"Ok".equals(obj.optString("code"))) return Optional.empty();
        JSONArray routes = obj.getJSONArray("routes");
        if (routes.isEmpty()) return Optional.empty();
        JSONArray coords = routes.getJSONObject(0)
            .getJSONObject("geometry")
            .getJSONArray("coordinates");
        List<GeoPosition> points = new ArrayList<>(coords.length());
        for (int i = 0; i < coords.length(); i++) {
            JSONArray pair = coords.getJSONArray(i);
            double lon = pair.getDouble(0);
            double lat = pair.getDouble(1);
            points.add(new GeoPosition(lat, lon));
        }
        return Optional.of(new Route(points));
    }
}
