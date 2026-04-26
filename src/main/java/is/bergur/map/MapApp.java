package is.bergur.map;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatLaf;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCursor;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;

import javax.swing.*;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.prefs.Preferences;

public class MapApp extends JFrame {

    private final Preferences prefs = Preferences.userNodeForPackage(MapApp.class);
    private static final String PREF_THEME = "theme";
    private static final String PREF_LAYER = "layer";
    private static final String PREF_WIN_X = "win_x";
    private static final String PREF_WIN_Y = "win_y";
    private static final String PREF_WIN_W = "win_w";
    private static final String PREF_WIN_H = "win_h";

    private enum Layer { STREET, SATELLITE }
    private static final String DEFAULT_THEME = "com.formdev.flatlaf.FlatDarculaLaf";

    private static final double FALLBACK_LAT = 64.1355;
    private static final double FALLBACK_LON = -21.8954;
    private static final int    DEFAULT_ZOOM = 5;

    private static final Map<String, String> THEMES = new LinkedHashMap<>();
    static {
        THEMES.put("Flat Light",      "com.formdev.flatlaf.FlatLightLaf");
        THEMES.put("Flat Dark",       "com.formdev.flatlaf.FlatDarkLaf");
        THEMES.put("IntelliJ",        "com.formdev.flatlaf.FlatIntelliJLaf");
        THEMES.put("Darcula",         "com.formdev.flatlaf.FlatDarculaLaf");
        THEMES.put("Arc Dark",        "com.formdev.flatlaf.intellijthemes.FlatArcDarkIJTheme");
        THEMES.put("Dracula",         "com.formdev.flatlaf.intellijthemes.FlatDraculaIJTheme");
        THEMES.put("Gruvbox Dark",    "com.formdev.flatlaf.intellijthemes.FlatGruvboxDarkMediumIJTheme");
        THEMES.put("Monokai Pro",     "com.formdev.flatlaf.intellijthemes.FlatMonokaiProIJTheme");
        THEMES.put("Moonlight",       "com.formdev.flatlaf.intellijthemes.FlatMoonlightIJTheme");
        THEMES.put("Nord",            "com.formdev.flatlaf.intellijthemes.FlatNordIJTheme");
        THEMES.put("One Dark",        "com.formdev.flatlaf.intellijthemes.FlatOneDarkIJTheme");
        THEMES.put("Solarized Dark",  "com.formdev.flatlaf.intellijthemes.FlatSolarizedDarkIJTheme");
        THEMES.put("Solarized Light", "com.formdev.flatlaf.intellijthemes.FlatSolarizedLightIJTheme");
        THEMES.put("Xcode Dark",      "com.formdev.flatlaf.intellijthemes.FlatXcodeDarkIJTheme");
    }

    // Fallback chain: specific → street only → valley → county seat
    private static final String[] DESTINATION_QUERIES = {
        "Vatnsendahlíð 100, Skorradalur, Iceland",
        "Vatnsendahlíð, Skorradalur, Iceland",
        "Skorradalur, Iceland",
        "Borgarnes, Iceland"
    };

    private JXMapViewer mapViewer;
    private JLabel statusLabel;
    private double homeLat = FALLBACK_LAT;
    private double homeLon = FALLBACK_LON;
    private Layer currentLayer = Layer.STREET;
    private final RoutePainter routePainter = new RoutePainter();
    private OsrmRouter.Route cachedRoute = null;

    public MapApp() {
        setTitle("Uthlid 16 - Reykjavik");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        int w = prefs.getInt(PREF_WIN_W, 900);
        int h = prefs.getInt(PREF_WIN_H, 700);
        int x = prefs.getInt(PREF_WIN_X, -1);
        int y = prefs.getInt(PREF_WIN_Y, -1);
        setSize(w, h);
        if (x >= 0 && y >= 0) setLocation(x, y);
        else setLocationRelativeTo(null);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                prefs.putInt(PREF_WIN_X, getX());
                prefs.putInt(PREF_WIN_Y, getY());
                prefs.putInt(PREF_WIN_W, getWidth());
                prefs.putInt(PREF_WIN_H, getHeight());
            }
        });

        currentLayer = Layer.valueOf(prefs.get(PREF_LAYER, Layer.STREET.name()));
        initComponents();
        setJMenuBar(createMenuBar());
        geocodeHome();
    }

    private void initComponents() {
        mapViewer = new JXMapViewer();

        DefaultTileFactory tileFactory = new DefaultTileFactory(tileFactoryInfoForLayer(currentLayer));
        tileFactory.setThreadPoolSize(8);
        mapViewer.setTileFactory(tileFactory);

        mapViewer.setAddressLocation(new GeoPosition(homeLat, homeLon));
        mapViewer.setZoom(DEFAULT_ZOOM);

        MouseInputListener mouseListener = new PanMouseInputListener(mapViewer);
        mapViewer.addMouseListener(mouseListener);
        mapViewer.addMouseMotionListener(mouseListener);
        mapViewer.addMouseWheelListener(new ZoomMouseWheelListenerCursor(mapViewer));
        mapViewer.setOverlayPainter(routePainter);

        statusLabel = new JLabel("Locating Uthlid 16...");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));

        setLayout(new BorderLayout());
        add(mapViewer, BorderLayout.CENTER);
        add(buildBottomPanel(), BorderLayout.SOUTH);
    }

    private JPanel buildBottomPanel() {
        JButton panN   = mapButton("N", () -> pan(0, -1));
        JButton panS   = mapButton("S", () -> pan(0, +1));
        JButton panW   = mapButton("W", () -> pan(-1, 0));
        JButton panE   = mapButton("E", () -> pan(+1, 0));
        JButton home   = mapButton("Home", this::goHome);
        JButton zoomIn = mapButton("+", () -> adjustZoom(-1));
        JButton zoomOut= mapButton("-", () -> adjustZoom(+1));

        JPanel panGrid = new JPanel(new GridLayout(3, 3, 2, 2));
        panGrid.add(new JLabel()); panGrid.add(panN);  panGrid.add(new JLabel());
        panGrid.add(panW);         panGrid.add(home);  panGrid.add(panE);
        panGrid.add(new JLabel()); panGrid.add(panS);  panGrid.add(new JLabel());

        JPanel zoomPanel = new JPanel(new GridLayout(2, 1, 2, 2));
        zoomPanel.add(zoomIn);
        zoomPanel.add(zoomOut);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 6));
        controls.add(panGrid);
        controls.add(zoomPanel);
        controls.add(statusLabel);

        return controls;
    }

    private JButton mapButton(String label, Runnable action) {
        JButton btn = new JButton(label);
        btn.setFont(btn.getFont().deriveFont(Font.PLAIN, 13f));
        btn.setPreferredSize(new Dimension(48, 36));
        btn.addActionListener(e -> action.run());
        return btn;
    }

    private void pan(int dx, int dy) {
        GeoPosition center = mapViewer.getCenterPosition();
        double delta = GeoUtils.panDelta(mapViewer.getZoom());
        mapViewer.setAddressLocation(new GeoPosition(
            center.getLatitude()  - dy * delta,
            center.getLongitude() + dx * delta
        ));
    }

    private void adjustZoom(int delta) {
        int z = Math.max(1, Math.min(17, mapViewer.getZoom() + delta));
        mapViewer.setZoom(z);
    }

    private void goHome() {
        mapViewer.setAddressLocation(new GeoPosition(homeLat, homeLon));
        mapViewer.setZoom(DEFAULT_ZOOM);
    }

    private void geocodeHome() {
        new SwingWorker<Optional<NominatimGeocoder.GeoResult>, Void>() {
            @Override
            protected Optional<NominatimGeocoder.GeoResult> doInBackground() throws Exception {
                return new NominatimGeocoder().searchFirst(
                    "Úthlíð 16, Reykjavík, Iceland",
                    "Úthlíð, Reykjavík, Iceland"
                );
            }
            @Override
            protected void done() {
                try {
                    Optional<NominatimGeocoder.GeoResult> result = get();
                    result.ifPresentOrElse(r -> {
                        homeLat = r.lat();
                        homeLon = r.lon();
                        mapViewer.setAddressLocation(new GeoPosition(homeLat, homeLon));
                        statusLabel.setText(String.format(java.util.Locale.US, "%.5f, %.5f", homeLat, homeLon));
                    }, () -> statusLabel.setText("Approx. location (geocode returned no results)"));
                } catch (Exception ex) {
                    statusLabel.setText("Approx. location (geocode failed)");
                }
            }
        }.execute();
    }

    private org.jxmapviewer.viewer.TileFactoryInfo tileFactoryInfoForLayer(Layer layer) {
        return layer == Layer.SATELLITE ? new EsriSatelliteTileFactoryInfo() : new OsmHttpsTileFactoryInfo();
    }

    private void switchLayer(Layer layer) {
        currentLayer = layer;
        prefs.put(PREF_LAYER, layer.name());
        GeoPosition center = mapViewer.getCenterPosition();
        int zoom = mapViewer.getZoom();
        DefaultTileFactory tf = new DefaultTileFactory(tileFactoryInfoForLayer(layer));
        tf.setThreadPoolSize(8);
        mapViewer.setTileFactory(tf);
        mapViewer.setAddressLocation(center);
        mapViewer.setZoom(zoom);
    }

    private JMenuBar createMenuBar() {
        JMenuBar bar = new JMenuBar();
        JMenu appearance = new JMenu("Appearance");
        JMenu themeMenu  = new JMenu("Theme");

        String current = prefs.get(PREF_THEME, DEFAULT_THEME);
        ButtonGroup group = new ButtonGroup();
        for (Map.Entry<String, String> e : THEMES.entrySet()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(e.getKey());
            item.setSelected(e.getValue().equals(current));
            item.addActionListener(ev -> switchTheme(e.getValue()));
            group.add(item);
            themeMenu.add(item);
        }
        appearance.add(themeMenu);
        bar.add(appearance);

        JMenu layerMenu = new JMenu("Layer");
        ButtonGroup layerGroup = new ButtonGroup();
        for (Layer layer : Layer.values()) {
            String label = layer == Layer.SATELLITE ? "Satellite" : "Street Map";
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(label);
            item.setSelected(layer == currentLayer);
            item.addActionListener(ev -> switchLayer(layer));
            layerGroup.add(item);
            layerMenu.add(item);
        }
        bar.add(layerMenu);

        JMenu routeMenu = new JMenu("Route");
        JCheckBoxMenuItem showRoute = new JCheckBoxMenuItem("Show route to Skorradalur");
        showRoute.addActionListener(ev -> {
            if (showRoute.isSelected()) fetchAndShowRoute();
            else hideRoute();
        });
        routeMenu.add(showRoute);
        bar.add(routeMenu);

        return bar;
    }

    private void hideRoute() {
        routePainter.clear();
        mapViewer.repaint();
    }

    private void fetchAndShowRoute() {
        if (cachedRoute != null) {
            applyRoute(cachedRoute);
            return;
        }
        statusLabel.setText("Fetching route...");

        new SwingWorker<OsrmRouter.Route, Void>() {
            @Override
            protected OsrmRouter.Route doInBackground() throws Exception {
                NominatimGeocoder geocoder = new NominatimGeocoder();
                NominatimGeocoder.GeoResult dest = geocoder.searchFirst(DESTINATION_QUERIES)
                    .orElseThrow(() -> new RuntimeException("Could not geocode destination (tried " + DESTINATION_QUERIES.length + " queries)"));
                GeoPosition from = new GeoPosition(homeLat, homeLon);
                GeoPosition to   = new GeoPosition(dest.lat(), dest.lon());
                return new OsrmRouter().fetchRoute(from, to)
                    .orElseThrow(() -> new RuntimeException("No route found"));
            }
            @Override
            protected void done() {
                try {
                    cachedRoute = get();
                    applyRoute(cachedRoute);
                } catch (Exception ex) {
                    statusLabel.setText("Route error: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void applyRoute(OsrmRouter.Route route) {
        routePainter.setTrack(route.points());
        mapViewer.zoomToBestFit(new java.util.HashSet<>(route.points()), 0.7);
        statusLabel.setText("Route: " + route.points().size() + " points");
    }

    private void switchTheme(String className) {
        prefs.put(PREF_THEME, className);
        try {
            UIManager.setLookAndFeel((LookAndFeel) Class.forName(className)
                .getDeclaredConstructor().newInstance());
            FlatLaf.updateUI();
        } catch (Exception ex) {
            // stay on current theme
        }
    }

    public static void main(String[] args) {
        System.setProperty("http.agent", "BergurOsmMapApp/1.0 bergur.heimisson@gmail.com");
        Preferences prefs = Preferences.userNodeForPackage(MapApp.class);
        try {
            String theme = prefs.get(PREF_THEME, DEFAULT_THEME);
            UIManager.setLookAndFeel((LookAndFeel) Class.forName(theme)
                .getDeclaredConstructor().newInstance());
        } catch (Exception e) {
            FlatDarculaLaf.setup();
        }
        SwingUtilities.invokeLater(() -> {
            MapApp app = new MapApp();
            app.setVisible(true);
            app.toFront();
            app.requestFocus();
        });
    }
}
