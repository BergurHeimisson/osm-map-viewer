# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```sh
mvn test              # run unit tests
mvn package           # compile + test + build fat JAR at target/osm-map-1.0-SNAPSHOT.jar
./run.sh              # launch the app (uses Homebrew Java 25)
```

## Architecture

Classes under `src/main/java/is/bergur/map/`:

- **`MapApp`** — JFrame entry point. Owns the JXMapViewer, button panel, theme menu, layer/overlay menus, and window-position persistence via `java.util.prefs.Preferences`. On startup fires a `SwingWorker` that calls `NominatimGeocoder` and re-centers the map. Contains `Layer` enum (STREET, SATELLITE, LMI) and `Overlay` enum (NONE, CYCLING, HIKING).
- **`NominatimGeocoder`** — Calls the Nominatim OSM geocoding API over `java.net.http.HttpClient`. `parseResponse()` and `buildUrl()` are package-private for unit testing.
- **`GeoUtils`** — Single static method `panDelta(zoom)` returning degree offset per button press, scaling with JXMapViewer zoom level.
- **`OsmHttpsTileFactoryInfo`** — TileFactoryInfo for OSM standard tiles (`tile.openstreetmap.org`).
- **`EsriSatelliteTileFactoryInfo`** — TileFactoryInfo for ESRI World Imagery tiles. Note: ESRI uses `z/y/x` URL order (not `z/x/y`).
- **`OpenTopoMapTileFactoryInfo`** — TileFactoryInfo for OpenTopoMap (`tile.opentopomap.org`). Topographic rendering of OSM data with contour lines.
- **`TileOverlayPainter`** — `Painter<JXMapViewer>` that fetches Waymarked Trails overlay tiles async and draws them on top of the base map. Uses a `ConcurrentHashMap` cache keyed by tile URL. `setUrlTemplate(String)` switches overlays (null = no-op). Static helpers `buildTileUrl` and `latLonToTileXY` are package-private for testing.
- **`LayeredPainter`** — Composes multiple `Painter<JXMapViewer>` instances, calling each in order. Used to stack `TileOverlayPainter` under `RoutePainter`.
- **`OsrmRouter`** — Fetches driving routes from the OSRM public API. Returns a list of `GeoPosition` points.
- **`RoutePainter`** — `Painter<JXMapViewer>` that draws a route polyline as an overlay.

## Key design notes

- **JXMapViewer zoom scale**: zoom 1 = most zoomed in (street), zoom 17 = world. `adjustZoom(+1)` zooms *out*.
- **Tile zoom inversion**: all `TileFactoryInfo` subclasses convert JX zoom to OSM zoom via `osm_z = MAX_ZOOM(17) - jxZoom`.
- **Pan direction math**: `pan(dx, dy)` uses `getCenterPosition()` and calls `setAddressLocation()` with the delta applied. North = latitude increase.
- **Geocode fallback**: `homeLat/homeLon` start at approximate Reykjavík center (64.1355, -21.8954). The geocoder result overwrites them.
- **Mouse interaction**: drag-to-pan and scroll-wheel-zoom via `PanMouseInputListener` and `ZoomMouseWheelListenerCursor` — buttons are supplementary.
- **Theme + window + layer + overlay** all persist across restarts via `Preferences.userNodeForPackage(MapApp.class)`.
- **Overlay tile fetching**: `TileOverlayPainter` uses daemon threads so in-flight fetches don't block JVM shutdown. Stale overlays are prevented by comparing `expectedTemplate` to `urlTemplate` before caching.
- **LMI Iceland caveat**: `gis.lmi.is` only serves EPSG:3057 tiles (Icelandic national projection), incompatible with JXMapViewer's Web Mercator system. OpenTopoMap is used instead for the topographic layer.

## Overlay tile sources

| Overlay | URL pattern |
|---------|-------------|
| Cycling | `https://tile.waymarkedtrails.org/cycling/{z}/{x}/{y}.png` |
| Hiking  | `https://tile.waymarkedtrails.org/hiking/{z}/{x}/{y}.png` |

## Dependencies

| Library | Purpose |
|---------|---------|
| `flatlaf` + `flatlaf-intellij-themes` 3.7 | Dark theme (Darcula default) + full theme switcher |
| `jxmapviewer2` 2.6 | OSM tile rendering in Swing |
| `org.json` 20240303 | Parse Nominatim JSON response |
| `junit-jupiter` 5.10.2 | Unit tests |
