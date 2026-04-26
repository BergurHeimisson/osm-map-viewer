# OSM Map Viewer

A Java Swing desktop app that displays an OpenStreetMap centered on Úthlíð 16, Reykjavík, with support for satellite imagery and driving-route overlay to Skorradalur.

## Features

- Street map (OpenStreetMap) and satellite imagery (Esri World Imagery) layers
- Driving route from Úthlíð 16 to Vatnsendahlíð 100, Skorradalur
- Pan (N/S/W/E buttons + drag) and zoom (buttons + scroll wheel)
- Home button to re-center on Úthlíð 16
- FlatLaf dark theme with 14-theme switcher
- Window position and last-used layer/theme remembered across restarts

## Requirements

- Java 25 (Homebrew: `/opt/homebrew/Cellar/openjdk/25.0.2`)
- Maven 3.9+

## Build

```sh
mvn package
```

Produces `target/osm-map-1.0-SNAPSHOT.jar` (fat JAR, all dependencies bundled).

## Run

```sh
chmod +x run.sh
./run.sh
```

## Menus

| Menu | Option | Effect |
|------|--------|--------|
| Layer | Street Map | OpenStreetMap tiles |
| Layer | Satellite | Esri World Imagery tiles |
| Route | Show route to Skorradalur | Fetch and draw driving route |
| Appearance | Theme | Switch between 14 FlatLaf themes |
