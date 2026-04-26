# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```sh
mvn test              # run unit tests
mvn package           # compile + test + build fat JAR at target/osm-map-1.0-SNAPSHOT.jar
./run.sh              # launch the app (uses Homebrew Java 25)
```

## Architecture

Three classes under `src/main/java/is/bergur/map/`:

- **`MapApp`** — JFrame entry point. Owns the JXMapViewer, button panel, theme menu, and window-position persistence via `java.util.prefs.Preferences`. On startup it fires a `SwingWorker` that calls `NominatimGeocoder` and re-centers the map once the exact coordinates arrive.
- **`NominatimGeocoder`** — Calls the Nominatim OSM geocoding API (`nominatim.openstreetmap.org`) over `java.net.http.HttpClient`. `parseResponse()` and `buildUrl()` are package-private so they can be unit-tested without HTTP.
- **`GeoUtils`** — Single static method `panDelta(zoom)` that returns the degree offset for one button press, scaling with JXMapViewer zoom level (zoom 1 = street level → tiny delta; zoom 17 = world view → large delta).

## Key design notes

- **JXMapViewer zoom scale**: zoom 1 = most zoomed in (street), zoom 17 = world. `adjustZoom(+1)` zooms *out*.
- **Pan direction math**: `pan(dx, dy)` uses `getCenterPosition()` and calls `setAddressLocation()` with the delta applied. North = latitude increase, so dy=-1 subtracts from dy giving `lat - (-1)*delta = lat + delta`. ✓
- **Geocode fallback**: `homeLat/homeLon` start at approximate Reykjavík center (64.1355, -21.8954). The geocoder result overwrites them; if geocoding fails, the Home button still returns to the fallback.
- **Mouse interaction**: drag-to-pan and scroll-wheel-zoom are wired via JXMapViewer's `PanMouseInputListener` and `ZoomMouseWheelListenerCursor` — buttons are supplementary.
- **Theme + window state** persist across restarts via `Preferences.userNodeForPackage(MapApp.class)`.

## Dependencies

| Library | Purpose |
|---------|---------|
| `flatlaf` + `flatlaf-intellij-themes` 3.7 | Dark theme (Darcula default) + full theme switcher |
| `jxmapviewer2` 2.6 | OSM tile rendering in Swing |
| `org.json` 20240303 | Parse Nominatim JSON response |
| `junit-jupiter` 5.10.2 | Unit tests |
