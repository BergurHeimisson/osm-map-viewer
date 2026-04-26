# Architecture

## Overview

A single-JFrame Swing app. On startup it geocodes Úthlíð 16 via Nominatim and centers the map there. Tile loading is handled by JXMapViewer2, which fetches OSM or Esri tiles on background threads. When the user toggles the route overlay, a SwingWorker geocodes the destination (with a 4-query fallback chain) and fetches a GeoJSON route from OSRM, then a custom `Painter` draws the polyline over the map on every repaint.

## Package layout

```
is.bergur.map
├── MapApp.java                  JFrame entry point; owns layout, menus, prefs, async workers
├── GeoUtils.java                Pan-delta calculation scaled by JXMapViewer zoom level
├── NominatimGeocoder.java       Geocoding via nominatim.openstreetmap.org; supports fallback-chain search
├── OsrmRouter.java              Driving routes via router.project-osrm.org (GeoJSON/OSRM API)
├── RoutePainter.java            Painter<JXMapViewer> that draws the route polyline + start/end markers
├── OsmHttpsTileFactoryInfo.java OSM tile factory using HTTPS (fixes redirect issue with default HTTP factory)
└── EsriSatelliteTileFactoryInfo.java  Esri World Imagery tile factory (z/y/x coordinate order)
```

## Key decisions

**HTTPS tile factory instead of `OSMTileFactoryInfo`**
JXMapViewer2's built-in `OSMTileFactoryInfo` uses `http://`. OSM now redirects to HTTPS but does not follow the redirect, so every tile silently fails (clock-icon placeholders). Fixed by subclassing `TileFactoryInfo` with the `https://` base URL directly.

**ESRI tile coordinate order**
Esri World Imagery uses `z/y/x` (row then column), not the `z/x/y` order OSM uses. Getting this wrong produces a grid of wrong tiles with no error.

**Geocoding fallback chain**
Rural Icelandic addresses may be absent or differently spelled in Nominatim. `searchFirst(String... queries)` tries each query in order with a 1.1 s delay between attempts (Nominatim ToS: max 1 req/s). The destination chain is: exact address → street only → valley name → county seat.

**Icelandic characters in query strings**
`URLEncoder.encode` with `StandardCharsets.UTF_8` handles `íð` etc. correctly; the strings must be written with the actual Unicode characters in source — ASCII transliterations like `Vatnsendahlid` do not match OSM data.

**Route caching**
The fetched route is cached in `cachedRoute`. Toggling the overlay off and back on reuses the cached result without another network round-trip.

**`http.agent` system property**
Set in `main()` before any network calls. OSM tile servers and Nominatim both require a descriptive User-Agent; without it requests may be rate-limited or rejected.

## External APIs (all free, no API key)

| Service | Purpose | URL |
|---------|---------|-----|
| Nominatim | Geocoding | `nominatim.openstreetmap.org` |
| OSRM | Driving routes | `router.project-osrm.org` |
| OpenStreetMap tiles | Street map | `tile.openstreetmap.org` |
| Esri World Imagery | Satellite map | `server.arcgisonline.com` |

## Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| `flatlaf` + `flatlaf-intellij-themes` | 3.7 | Dark theme (Darcula default) + theme switcher |
| `jxmapviewer2` | 2.6 | OSM tile rendering and geo utilities in Swing |
| `org.json` | 20240303 | Parse Nominatim and OSRM JSON responses |
| `junit-jupiter` | 5.10.2 | Unit tests |

## Launcher script (`run.sh`)

Uses the Homebrew Java 25 path directly rather than `java_home`, which may resolve to an older system JVM. The `--enable-native-access=ALL-UNNAMED` flag suppresses the FlatLaf native-library warning on Java 25.
