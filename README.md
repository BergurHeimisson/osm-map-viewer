# OSM Map Viewer

A desktop map viewer built with Java Swing and OpenStreetMap tiles. Shows a map centred on Reykjavík with pan, zoom, layer switching, overlay toggling, and driving route display.

## Features

- **Three base layers** — Street Map (OSM), Satellite (ESRI), OpenTopoMap (topographic)
- **Two overlay layers** — Waymarked Trails cycling and hiking routes, rendered on top of any base layer
- **Routing** — Fetches a driving route to Skorradalur via the OSRM public API and draws it as a polyline
- **Theme switcher** — 14 FlatLaf themes including Darcula, Nord, One Dark, Gruvbox Dark
- **Persistent state** — Window position, size, theme, base layer, and overlay all saved across restarts

## Requirements

- Java 25 (Homebrew: `brew install openjdk`)
- Maven 3.x

## Build & Run

```sh
mvn package        # compile, test, build fat JAR
./run.sh           # launch the app
```

Run tests only:

```sh
mvn test
```

## Controls

| Control | Action |
|---------|--------|
| N / S / W / E buttons | Pan the map |
| + / − buttons | Zoom in / out |
| Home button | Return to Reykjavík |
| Scroll wheel | Zoom |
| Click + drag | Pan |
| Layer menu → base | Switch between Street Map, Satellite, OpenTopoMap |
| Layer menu → overlay | Toggle None / Cycling / Hiking routes |
| Route menu | Show/hide driving route to Skorradalur |
| Appearance menu | Switch UI theme |

## Architecture

```
MapApp               — JFrame, menus, button panel, Preferences persistence
├── OsmHttpsTileFactoryInfo      — OSM tile URLs
├── EsriSatelliteTileFactoryInfo — ESRI satellite tile URLs
├── OpenTopoMapTileFactoryInfo   — OpenTopoMap tile URLs
├── TileOverlayPainter           — async overlay tile fetch + render
├── LayeredPainter               — composes overlay + route painters
├── RoutePainter                 — draws route polyline
├── NominatimGeocoder            — geocodes addresses via Nominatim
└── OsrmRouter                   — fetches driving routes via OSRM
```

## Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| [JXMapViewer2](https://github.com/msteiger/jxmapviewer2) | 2.6 | OSM tile rendering in Swing |
| [FlatLaf](https://www.formdev.com/flatlaf/) + intellij-themes | 3.7 | UI themes |
| [org.json](https://github.com/stleary/JSON-java) | 20240303 | JSON parsing |
| JUnit Jupiter | 5.10.2 | Unit tests |

## Tile sources

- **Street Map** — [OpenStreetMap](https://www.openstreetmap.org/) (`tile.openstreetmap.org`)
- **Satellite** — ESRI World Imagery (`server.arcgisonline.com`)
- **OpenTopoMap** — [OpenTopoMap](https://opentopomap.org/) (`tile.opentopomap.org`)
- **Cycling overlay** — [Waymarked Trails](https://cycling.waymarkedtrails.org/) (`tile.waymarkedtrails.org/cycling`)
- **Hiking overlay** — [Waymarked Trails](https://hiking.waymarkedtrails.org/) (`tile.waymarkedtrails.org/hiking`)
- **Geocoding** — [Nominatim](https://nominatim.openstreetmap.org/) (OpenStreetMap)
- **Routing** — [OSRM](https://router.project-osrm.org/) public demo server
