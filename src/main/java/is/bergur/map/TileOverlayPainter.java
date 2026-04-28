package is.bergur.map;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.GeoPosition;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TileOverlayPainter implements Painter<JXMapViewer> {

    private static final int TILE_SIZE = 256;
    private static final int MAX_ZOOM = 17;

    private volatile String urlTemplate = null;
    private final ConcurrentHashMap<String, BufferedImage> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> inFlight = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(4,
        r -> { Thread t = new Thread(r); t.setDaemon(true); return t; });
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public void setUrlTemplate(String template) {
        urlTemplate = template;
        cache.clear();
        inFlight.clear();
    }

    @Override
    public void paint(Graphics2D g, JXMapViewer map, int w, int h) {
        String template = urlTemplate;
        if (template == null) return;

        GeoPosition center = map.getCenterPosition();
        int z = MAX_ZOOM - map.getZoom();
        if (z < 0 || z > 16) return;

        double[] xy = latLonToTileXY(center.getLatitude(), center.getLongitude(), z);
        int centerTileX = (int) xy[0];
        int centerTileY = (int) xy[1];
        double pixOffX = (xy[0] - centerTileX) * TILE_SIZE;
        double pixOffY = (xy[1] - centerTileY) * TILE_SIZE;

        int rangeX = (int) Math.ceil((double) w / 2 / TILE_SIZE) + 1;
        int rangeY = (int) Math.ceil((double) h / 2 / TILE_SIZE) + 1;
        int maxTile = (1 << z) - 1;

        for (int tx = centerTileX - rangeX; tx <= centerTileX + rangeX; tx++) {
            for (int ty = centerTileY - rangeY; ty <= centerTileY + rangeY; ty++) {
                if (ty < 0 || ty > maxTile) continue;
                int wrappedTx = Math.floorMod(tx, maxTile + 1);
                String url = buildTileUrl(template, wrappedTx, ty, z);
                int drawX = (int) (w / 2.0 - pixOffX + (tx - centerTileX) * TILE_SIZE);
                int drawY = (int) (h / 2.0 - pixOffY + (ty - centerTileY) * TILE_SIZE);

                BufferedImage img = cache.get(url);
                if (img != null) {
                    g.drawImage(img, drawX, drawY, null);
                } else if (inFlight.putIfAbsent(url, Boolean.TRUE) == null) {
                    fetchTile(url, map, template);
                }
            }
        }
    }

    private void fetchTile(String url, JXMapViewer map, String expectedTemplate) {
        executor.submit(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "BergurOsmMapApp/1.0 bergur.heimisson@gmail.com")
                    .build();
                HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
                if (resp.statusCode() == 200
                        && expectedTemplate.equals(urlTemplate)) {
                    BufferedImage img = ImageIO.read(new ByteArrayInputStream(resp.body()));
                    if (img != null) {
                        cache.put(url, img);
                        map.repaint();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception ignored) {
            } finally {
                inFlight.remove(url);
            }
        });
    }

    static String buildTileUrl(String template, int x, int y, int z) {
        return template
            .replace("{z}", String.valueOf(z))
            .replace("{x}", String.valueOf(x))
            .replace("{y}", String.valueOf(y));
    }

    static double[] latLonToTileXY(double lat, double lon, int z) {
        double x = (lon + 180.0) / 360.0 * (1 << z);
        double latRad = Math.toRadians(lat);
        double y = (1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * (1 << z);
        return new double[]{x, y};
    }
}
