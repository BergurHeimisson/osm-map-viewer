# Layer Switcher Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add LMI Iceland as a base layer and Waymarked Trails cycling/hiking as toggleable overlays.

**Architecture:** A new `TileOverlayPainter` fetches overlay tiles async, caches them in a `ConcurrentHashMap`, and draws them over the base map via JXMapViewer's painter API. A `LayeredPainter` composes it with the existing `RoutePainter`. `MapApp` gains an `Overlay` enum and a radio-button group in the Layer menu.

**Tech Stack:** Java 25, JXMapViewer2, FlatLaf, JUnit Jupiter 5, `java.net.http.HttpClient`

---

## File Map

| Action | File |
|--------|------|
| Create | `src/main/java/is/bergur/map/LmiIcelandTileFactoryInfo.java` |
| Create | `src/main/java/is/bergur/map/TileOverlayPainter.java` |
| Create | `src/main/java/is/bergur/map/LayeredPainter.java` |
| Modify | `src/main/java/is/bergur/map/MapApp.java` |
| Create | `src/test/java/is/bergur/map/LmiIcelandTileFactoryInfoTest.java` |
| Create | `src/test/java/is/bergur/map/TileOverlayPainterTest.java` |

---

## Task 1: LmiIcelandTileFactoryInfo

**Files:**
- Create: `src/main/java/is/bergur/map/LmiIcelandTileFactoryInfo.java`
- Create: `src/test/java/is/bergur/map/LmiIcelandTileFactoryInfoTest.java`

- [ ] **Step 1: Write the failing tests**

Create `src/test/java/is/bergur/map/LmiIcelandTileFactoryInfoTest.java`:

```java
package is.bergur.map;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LmiIcelandTileFactoryInfoTest {

    private final LmiIcelandTileFactoryInfo info = new LmiIcelandTileFactoryInfo();

    @Test
    void urlUsesHttps() {
        assertTrue(info.getTileUrl(10, 20, 5).startsWith("https://"));
    }

    @Test
    void urlContainsLmiHost() {
        assertTrue(info.getTileUrl(10, 20, 5).contains("wmts.lmi.is"));
    }

    @Test
    void urlOrderIsZXY() {
        int x = 10, y = 20, jxZoom = 5;
        int expectedZ = 17 - jxZoom; // = 12
        String url = info.getTileUrl(x, y, jxZoom);
        assertTrue(url.endsWith("/" + expectedZ + "/" + x + "/" + y + ".png"),
            "expected /{z}/{x}/{y}.png suffix in: " + url);
    }
}
```

- [ ] **Step 2: Run tests — verify they fail**

```bash
mvn test -pl . -Dtest=LmiIcelandTileFactoryInfoTest -q
```

Expected: compile error — `LmiIcelandTileFactoryInfo` does not exist yet.

- [ ] **Step 3: Implement LmiIcelandTileFactoryInfo**

Create `src/main/java/is/bergur/map/LmiIcelandTileFactoryInfo.java`:

```java
package is.bergur.map;

import org.jxmapviewer.viewer.TileFactoryInfo;

public class LmiIcelandTileFactoryInfo extends TileFactoryInfo {

    private static final int MAX_ZOOM = 17;
    private static final String BASE_URL =
        "https://wmts.lmi.is/mapcache/wmts/1.0.0/LMI_Stafraen_Uppdrattur/default/GoogleMapsCompatible/";

    public LmiIcelandTileFactoryInfo() {
        super(1, MAX_ZOOM - 2, MAX_ZOOM, 256, true, true, BASE_URL, "x", "y", "zoom");
    }

    @Override
    public String getTileUrl(int x, int y, int zoom) {
        int z = MAX_ZOOM - zoom;
        return BASE_URL + z + "/" + x + "/" + y + ".png";
    }
}
```

- [ ] **Step 4: Run tests — verify they pass**

```bash
mvn test -pl . -Dtest=LmiIcelandTileFactoryInfoTest -q
```

Expected: `BUILD SUCCESS`, 3 tests passed.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/is/bergur/map/LmiIcelandTileFactoryInfo.java \
        src/test/java/is/bergur/map/LmiIcelandTileFactoryInfoTest.java
git commit -m "feat: add LmiIcelandTileFactoryInfo for WMTS base layer"
```

---

## Task 2: TileOverlayPainter — URL builder and tile math

**Files:**
- Create: `src/main/java/is/bergur/map/TileOverlayPainter.java` (skeleton + two static helpers)
- Create: `src/test/java/is/bergur/map/TileOverlayPainterTest.java`

- [ ] **Step 1: Write the failing tests**

Create `src/test/java/is/bergur/map/TileOverlayPainterTest.java`:

```java
package is.bergur.map;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TileOverlayPainterTest {

    @Test
    void buildTileUrl_replacesAllPlaceholders() {
        String template = "https://tile.waymarkedtrails.org/cycling/{z}/{x}/{y}.png";
        assertEquals(
            "https://tile.waymarkedtrails.org/cycling/5/10/15.png",
            TileOverlayPainter.buildTileUrl(template, 10, 15, 5)
        );
    }

    @Test
    void latLonToTileXY_reykjavik_zoom1() {
        // Reykjavik (64.1355°N, 21.8954°W) at OSM zoom 1 → tile (0, 0)
        // fractional x ≈ 0.878, fractional y ≈ 0.535
        double[] xy = TileOverlayPainter.latLonToTileXY(64.1355, -21.8954, 1);
        assertEquals(0, (int) xy[0], "tile x should be 0");
        assertEquals(0, (int) xy[1], "tile y should be 0");
        assertTrue(xy[0] > 0.8 && xy[0] < 0.9, "fractional x ≈ 0.878, got " + xy[0]);
        assertTrue(xy[1] > 0.5 && xy[1] < 0.6, "fractional y ≈ 0.535, got " + xy[1]);
    }

    @Test
    void latLonToTileXY_greenwich_equator_zoom1() {
        // (0°, 0°) is at tile position (1.0, 1.0) — exact boundary between 4 tiles
        double[] xy = TileOverlayPainter.latLonToTileXY(0.0, 0.0, 1);
        assertEquals(1.0, xy[0], 0.001);
        assertEquals(1.0, xy[1], 0.001);
    }

    @Test
    void setUrlTemplate_doesNotThrow() {
        TileOverlayPainter painter = new TileOverlayPainter();
        painter.setUrlTemplate("https://tile.waymarkedtrails.org/cycling/{z}/{x}/{y}.png");
        painter.setUrlTemplate(null);
    }
}
```

- [ ] **Step 2: Run tests — verify they fail**

```bash
mvn test -pl . -Dtest=TileOverlayPainterTest -q
```

Expected: compile error — `TileOverlayPainter` does not exist yet.

- [ ] **Step 3: Implement TileOverlayPainter skeleton with static helpers**

Create `src/main/java/is/bergur/map/TileOverlayPainter.java`:

```java
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
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
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
        if (z < 0 || z > 18) return;

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
```

- [ ] **Step 4: Run tests — verify they pass**

```bash
mvn test -pl . -Dtest=TileOverlayPainterTest -q
```

Expected: `BUILD SUCCESS`, 4 tests passed.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/is/bergur/map/TileOverlayPainter.java \
        src/test/java/is/bergur/map/TileOverlayPainterTest.java
git commit -m "feat: add TileOverlayPainter with tile math and async fetch"
```

---

## Task 3: LayeredPainter

**Files:**
- Create: `src/main/java/is/bergur/map/LayeredPainter.java`

`LayeredPainter` is a trivial loop — no logic worth unit testing in isolation. It's covered by the full integration test (running the app). Just implement it.

- [ ] **Step 1: Create LayeredPainter**

Create `src/main/java/is/bergur/map/LayeredPainter.java`:

```java
package is.bergur.map;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.painter.Painter;

import java.awt.Graphics2D;

public class LayeredPainter implements Painter<JXMapViewer> {

    private final Painter<JXMapViewer>[] painters;

    @SafeVarargs
    public LayeredPainter(Painter<JXMapViewer>... painters) {
        this.painters = painters;
    }

    @Override
    public void paint(Graphics2D g, JXMapViewer map, int w, int h) {
        for (Painter<JXMapViewer> painter : painters) {
            painter.paint(g, map, w, h);
        }
    }
}
```

- [ ] **Step 2: Run all tests to make sure nothing is broken**

```bash
mvn test -q
```

Expected: `BUILD SUCCESS`, all existing tests pass.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/is/bergur/map/LayeredPainter.java
git commit -m "feat: add LayeredPainter to compose overlay and route painters"
```

---

## Task 4: Wire everything into MapApp

**Files:**
- Modify: `src/main/java/is/bergur/map/MapApp.java`

- [ ] **Step 1: Add Overlay enum, PREF_OVERLAY constant, and new fields**

In `MapApp.java`, add the following inside the class (after the existing `Layer` enum and constants):

```java
private static final String PREF_OVERLAY = "overlay";

private enum Overlay {
    NONE(null),
    CYCLING("https://tile.waymarkedtrails.org/cycling/{z}/{x}/{y}.png"),
    HIKING ("https://tile.waymarkedtrails.org/hiking/{z}/{x}/{y}.png");

    final String urlTemplate;
    Overlay(String urlTemplate) { this.urlTemplate = urlTemplate; }
}

private Overlay currentOverlay = Overlay.NONE;
private final TileOverlayPainter tileOverlayPainter = new TileOverlayPainter();
```

- [ ] **Step 2: Load overlay preference in constructor**

In the `MapApp()` constructor, after the line that loads `currentLayer`:

```java
currentLayer = Layer.valueOf(prefs.get(PREF_LAYER, Layer.STREET.name()));
// add this line:
currentOverlay = Overlay.valueOf(prefs.get(PREF_OVERLAY, Overlay.NONE.name()));
```

- [ ] **Step 3: Wire LayeredPainter in initComponents**

In `initComponents()`, replace:
```java
mapViewer.setOverlayPainter(routePainter);
```
with:
```java
tileOverlayPainter.setUrlTemplate(currentOverlay.urlTemplate);
mapViewer.setOverlayPainter(new LayeredPainter(tileOverlayPainter, routePainter));
```

- [ ] **Step 4: Add switchOverlay method**

Add this method after `switchLayer()`:

```java
private void switchOverlay(Overlay overlay) {
    currentOverlay = overlay;
    prefs.put(PREF_OVERLAY, overlay.name());
    tileOverlayPainter.setUrlTemplate(overlay.urlTemplate);
    mapViewer.repaint();
}
```

- [ ] **Step 5: Add LMI to Layer enum and tileFactoryInfoForLayer**

Change:
```java
private enum Layer { STREET, SATELLITE }
```
to:
```java
private enum Layer { STREET, SATELLITE, LMI }
```

Replace `tileFactoryInfoForLayer`:
```java
private org.jxmapviewer.viewer.TileFactoryInfo tileFactoryInfoForLayer(Layer layer) {
    return switch (layer) {
        case SATELLITE -> new EsriSatelliteTileFactoryInfo();
        case LMI       -> new LmiIcelandTileFactoryInfo();
        default        -> new OsmHttpsTileFactoryInfo();
    };
}
```

- [ ] **Step 6: Update createMenuBar — base layer labels and overlay group**

In `createMenuBar()`, replace the inner loop that builds layer menu items:

```java
JMenu layerMenu = new JMenu("Layer");

// Base layer group
ButtonGroup baseGroup = new ButtonGroup();
for (Layer layer : Layer.values()) {
    String label = switch (layer) {
        case SATELLITE -> "Satellite";
        case LMI       -> "LMI Iceland";
        default        -> "Street Map";
    };
    JRadioButtonMenuItem item = new JRadioButtonMenuItem(label);
    item.setSelected(layer == currentLayer);
    item.addActionListener(ev -> switchLayer(layer));
    baseGroup.add(item);
    layerMenu.add(item);
}

// Overlay group
layerMenu.add(new JSeparator());
ButtonGroup overlayGroup = new ButtonGroup();
for (Overlay overlay : Overlay.values()) {
    String label = switch (overlay) {
        case CYCLING -> "Cycling";
        case HIKING  -> "Hiking";
        default      -> "None";
    };
    JRadioButtonMenuItem item = new JRadioButtonMenuItem(label);
    item.setSelected(overlay == currentOverlay);
    item.addActionListener(ev -> switchOverlay(overlay));
    overlayGroup.add(item);
    layerMenu.add(item);
}
bar.add(layerMenu);
```

Note: remove the `bar.add(layerMenu)` that was already in the original code since the replacement block includes it.

- [ ] **Step 7: Run all tests**

```bash
mvn test -q
```

Expected: `BUILD SUCCESS`, all tests pass.

- [ ] **Step 8: Build and run the app**

```bash
mvn package -q && ./run.sh
```

Manual checks:
1. Layer menu shows: Street Map / Satellite / LMI Iceland (base), then separator, then None / Cycling / Hiking (overlay)
2. Switching to "LMI Iceland" renders Icelandic map tiles in Icelandic
3. Selecting "Cycling" overlays coloured cycle routes on any base map
4. Selecting "Hiking" shows marked hiking trails
5. Selecting "None" clears the overlay
6. Restarting the app restores the last-used base layer and overlay

- [ ] **Step 9: Commit**

```bash
git add src/main/java/is/bergur/map/MapApp.java .gitignore
git commit -m "feat: add LMI Iceland base layer and Waymarked Trails overlays"
```
