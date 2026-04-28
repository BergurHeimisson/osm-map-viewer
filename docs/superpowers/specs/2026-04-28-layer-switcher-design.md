# Layer Switcher Design

**Date:** 2026-04-28
**Status:** Approved

## Goal

Add an LMI Iceland base layer and independently-selectable Waymarked Trails overlays (cycling, hiking) to the existing OSM map viewer.

## Scope

- 1 new base layer: LMI Iceland (Landmælingar Íslands WMTS)
- 2 overlay layers: Waymarked Trails Cycling, Waymarked Trails Hiking
- Overlays are radio-button style: None / Cycling / Hiking (one active at a time)
- Overlay + base layer selections persist across restarts via `Preferences`

## Tile Sources

| Layer | URL pattern |
|-------|-------------|
| LMI Iceland (base) | `https://wmts.lmi.is/mapcache/wmts/1.0.0/LMI_Stafraen_Uppdrattur/default/GoogleMapsCompatible/{z}/{x}/{y}.png` |
| Cycling overlay | `https://tile.waymarkedtrails.org/cycling/{z}/{x}/{y}.png` |
| Hiking overlay | `https://tile.waymarkedtrails.org/hiking/{z}/{x}/{y}.png` |

Waymarked Trails tiles have transparent backgrounds — they layer cleanly over any base map.

## New Files

### `LmiIcelandTileFactoryInfo.java`

Extends `TileFactoryInfo` like the existing `OsmHttpsTileFactoryInfo`. OSM zoom formula: `osm_z = MAX_ZOOM - jxZoom`. WMTS endpoint uses `{z}/{x}/{y}` path order.

### `TileOverlayPainter.java`

Implements `Painter<JXMapViewer>`. Responsible for fetching, caching, and rendering overlay tiles.

**Tile math:** Convert viewport lat/lon corners to OSM tile coordinates using standard Web Mercator formula. Iterate over all visible tile positions.

**Cache:** `ConcurrentHashMap<String, BufferedImage>` keyed by tile URL. Three states per tile:
- Cached → draw immediately
- In-flight → skip (repaint triggered on completion)
- Unseen → submit to `ExecutorService` (4 threads); on completion store + call `mapViewer.repaint()`

Cache cleared on overlay switch. No eviction policy needed for this scope.

**Rendering:** In `paint(Graphics2D g, JXMapViewer map, int w, int h)`, compute each visible tile's pixel offset relative to viewport centre and draw its `BufferedImage`.

**No-op mode:** When overlay is `NONE`, the painter's URL template is null and `paint()` returns immediately.

### `LayeredPainter.java`

Trivial `Painter<JXMapViewer>` wrapper holding `[tileOverlayPainter, routePainter]` and calling `paint()` on each in order. Replaces the current direct `setOverlayPainter(routePainter)` call.

## Changes to `MapApp.java`

- Add `Layer.LMI` to the `Layer` enum; update `tileFactoryInfoForLayer()` to return `LmiIcelandTileFactoryInfo`
- Add `Overlay` enum: `NONE, CYCLING, HIKING`
- Add `currentOverlay` field (default `NONE`), loaded from and saved to `Preferences` (`PREF_OVERLAY` key)
- Add `switchOverlay(Overlay)`: updates `currentOverlay`, prefs, and calls `tileOverlayPainter.setOverlay(overlay)` + `mapViewer.repaint()`
- Replace `setOverlayPainter(routePainter)` with `setOverlayPainter(new LayeredPainter(tileOverlayPainter, routePainter))`
- Expand Layer menu: existing base radio buttons gain `LMI Iceland`; add `JSeparator`; add Overlay radio group with `None / Cycling / Hiking`

## UI

```
Menu bar → Layer
  [Base]
  ● Street Map
  ○ Satellite
  ○ LMI Iceland
  ──────────────
  [Overlay]
  ● None
  ○ Cycling
  ○ Hiking
```

Both groups use `JRadioButtonMenuItem` with separate `ButtonGroup`s. No other UI changes.

## Out of Scope

- Multiple overlays simultaneously
- OpenTopoMap base layer
- Overlay opacity control
- Tile cache size limit / eviction
