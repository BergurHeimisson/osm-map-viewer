package is.bergur.map;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatLaf;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCursor;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.prefs.Preferences;

public class MapApp extends JFrame {

    private final Preferences prefs = Preferences.userNodeForPackage(MapApp.class);
    private static final String PREF_THEME = "theme";
    private static final String PREF_LAYER = "layer";
    private static final String PREF_WIN_X = "win_x";
    private static final String PREF_WIN_Y = "win_y";
    private static final String PREF_WIN_W = "win_w";
    private static final String PREF_WIN_H = "win_h";

    private static final String PREF_OVERLAY = "overlay";
    private static final String PREF_LAST_PHOTO_DIR = "last_photo_dir";

    private enum Layer { STREET, SATELLITE, LMI }

    private enum Overlay {
        NONE(null),
        CYCLING("https://tile.waymarkedtrails.org/cycling/{z}/{x}/{y}.png"),
        HIKING ("https://tile.waymarkedtrails.org/hiking/{z}/{x}/{y}.png");

        final String urlTemplate;
        Overlay(String urlTemplate) { this.urlTemplate = urlTemplate; }
    }

    private static final String DEFAULT_THEME = "com.formdev.flatlaf.FlatDarculaLaf";

    private Overlay currentOverlay;
    private final TileOverlayPainter tileOverlayPainter = new TileOverlayPainter();

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
        currentOverlay = Overlay.valueOf(prefs.get(PREF_OVERLAY, Overlay.NONE.name()));
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
        tileOverlayPainter.setUrlTemplate(currentOverlay.urlTemplate);
        mapViewer.setOverlayPainter(new LayeredPainter(tileOverlayPainter, routePainter));

        statusLabel = new JLabel("Locating Uthlid 16...");

        mapViewer.addPropertyChangeListener("centerPosition", evt -> {
            GeoPosition pos = (GeoPosition) evt.getNewValue();
            if (pos != null) {
                statusLabel.setText(String.format(java.util.Locale.US, "%.5f, %.5f",
                    pos.getLatitude(), pos.getLongitude()));
            }
        });
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

        JButton copyBtn = new JButton("Copy");
        copyBtn.setFont(copyBtn.getFont().deriveFont(Font.PLAIN, 11f));
        copyBtn.setMargin(new Insets(2, 6, 2, 6));
        copyBtn.addActionListener(e -> {
            java.awt.datatransfer.StringSelection sel =
                new java.awt.datatransfer.StringSelection(statusLabel.getText());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, sel);
        });

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 6));
        controls.add(panGrid);
        controls.add(zoomPanel);
        controls.add(statusLabel);
        controls.add(copyBtn);

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
        return switch (layer) {
            case STREET    -> new OsmHttpsTileFactoryInfo();
            case SATELLITE -> new EsriSatelliteTileFactoryInfo();
            case LMI       -> new OpenTopoMapTileFactoryInfo();
        };
    }

    private void switchOverlay(Overlay overlay) {
        currentOverlay = overlay;
        prefs.put(PREF_OVERLAY, overlay.name());
        tileOverlayPainter.setUrlTemplate(overlay.urlTemplate);
        mapViewer.repaint();
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

        // Base layer group
        ButtonGroup baseGroup = new ButtonGroup();
        for (Layer layer : Layer.values()) {
            String label = switch (layer) {
                case STREET    -> "Street Map";
                case SATELLITE -> "Satellite";
                case LMI       -> "OpenTopoMap";
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
                case NONE    -> "None";
                case CYCLING -> "Cycling";
                case HIKING  -> "Hiking";
            };
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(label);
            item.setSelected(overlay == currentOverlay);
            item.addActionListener(ev -> switchOverlay(overlay));
            overlayGroup.add(item);
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

        JMenu photoMenu = new JMenu("Photo");
        JMenuItem openPhoto = new JMenuItem("Open Photo...");
        openPhoto.addActionListener(ev -> openPhotoAndLocate());
        photoMenu.add(openPhoto);
        bar.add(photoMenu);

        return bar;
    }

    private void openPhotoAndLocate() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select a photo");
        String lastDir = prefs.get(PREF_LAST_PHOTO_DIR, null);
        if (lastDir != null) chooser.setCurrentDirectory(new File(lastDir));
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        for (String ext : new String[]{"jpg", "jpeg", "heic", "heif", "tif", "tiff", "png", "dng", "cr3", "cr2", "nef", "arw", "raf"}) {
            chooser.addChoosableFileFilter(
                new javax.swing.filechooser.FileNameExtensionFilter(ext.toUpperCase() + " files", ext));
        }
        ImagePreviewPanel preview = new ImagePreviewPanel();
        chooser.setAccessory(preview);
        chooser.addPropertyChangeListener(preview);

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File selected = chooser.getSelectedFile();
        prefs.put(PREF_LAST_PHOTO_DIR, selected.getParent());

        PhotoLocator.readGps(selected).ifPresentOrElse(pos -> {
            mapViewer.setAddressLocation(pos);
            mapViewer.setZoom(3);
        }, () -> JOptionPane.showMessageDialog(this,
            "No GPS coordinates found in this photo.",
            "No Location", JOptionPane.INFORMATION_MESSAGE));
    }

    private class ImagePreviewPanel extends JPanel implements java.beans.PropertyChangeListener {
        private static final int SIZE = 220;
        private static final Set<String> RAW_EXTS = new HashSet<>(
            Arrays.asList("cr3", "cr2", "nef", "arw", "raf", "dng"));

        private BufferedImage thumb;
        private String status = "No image selected";
        private SwingWorker<BufferedImage, Void> currentWorker;

        ImagePreviewPanel() {
            setPreferredSize(new Dimension(SIZE + 20, SIZE + 20));
            setBorder(BorderFactory.createTitledBorder("Preview"));
        }

        @Override
        public void propertyChange(java.beans.PropertyChangeEvent evt) {
            if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(evt.getPropertyName())) {
                load((File) evt.getNewValue());
            }
        }

        private void load(File file) {
            if (currentWorker != null) currentWorker.cancel(true);
            thumb = null;
            if (file == null || !file.isFile()) {
                status = "No image selected";
                repaint();
                return;
            }
            status = "Loading...";
            repaint();
            currentWorker = new SwingWorker<>() {
                @Override
                protected BufferedImage doInBackground() throws Exception {
                    String ext = ext(file);
                    return RAW_EXTS.contains(ext) ? loadRaw(file) : loadStandard(file);
                }
                @Override
                protected void done() {
                    if (!isCancelled()) {
                        try { thumb = get(); } catch (Exception ignored) {}
                        status = thumb != null ? "" : "Cannot preview";
                        repaint();
                    }
                }
            };
            currentWorker.execute();
        }

        private BufferedImage loadStandard(File f) throws Exception {
            BufferedImage img = ImageIO.read(f);
            return img != null ? scale(img) : null;
        }

        private BufferedImage loadRaw(File f) throws Exception {
            File tmp = File.createTempFile("map_prev_", ".jpg");
            tmp.deleteOnExit();
            try {
                new ProcessBuilder(
                    "sips", "-s", "format", "jpeg",
                    "-z", String.valueOf(SIZE * 2), String.valueOf(SIZE * 2),
                    f.getAbsolutePath(), "--out", tmp.getAbsolutePath()
                ).redirectErrorStream(true).start().waitFor();
                if (tmp.length() > 0) {
                    BufferedImage img = ImageIO.read(tmp);
                    return img != null ? scale(img) : null;
                }
            } finally {
                tmp.delete();
            }
            return null;
        }

        private BufferedImage scale(BufferedImage img) {
            int w = img.getWidth(), h = img.getHeight();
            if (w <= SIZE && h <= SIZE) return img;
            float s = Math.min((float) SIZE / w, (float) SIZE / h);
            int nw = Math.round(w * s), nh = Math.round(h * s);
            BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(img, 0, 0, nw, nh, null);
            g.dispose();
            return out;
        }

        private String ext(File f) {
            String n = f.getName().toLowerCase();
            int i = n.lastIndexOf('.');
            return i >= 0 ? n.substring(i + 1) : "";
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int w = getWidth(), h = getHeight();
            if (thumb != null) {
                g.drawImage(thumb, (w - thumb.getWidth()) / 2, (h - thumb.getHeight()) / 2, null);
            } else {
                FontMetrics fm = g.getFontMetrics();
                g.drawString(status, (w - fm.stringWidth(status)) / 2, h / 2);
            }
        }
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
