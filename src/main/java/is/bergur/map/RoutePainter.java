package is.bergur.map;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.GeoPosition;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.List;

public class RoutePainter implements Painter<JXMapViewer> {

    private List<GeoPosition> track = List.of();

    public void setTrack(List<GeoPosition> track) {
        this.track = List.copyOf(track);
    }

    public void clear() {
        this.track = List.of();
    }

    @Override
    public void paint(Graphics2D g, JXMapViewer map, int width, int height) {
        if (track.size() < 2) return;

        g = (Graphics2D) g.create();
        Rectangle viewport = map.getViewportBounds();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int[] xs = new int[track.size()];
        int[] ys = new int[track.size()];
        for (int i = 0; i < track.size(); i++) {
            Point2D pt = map.getTileFactory().geoToPixel(track.get(i), map.getZoom());
            xs[i] = (int) (pt.getX() - viewport.getX());
            ys[i] = (int) (pt.getY() - viewport.getY());
        }

        // Route line
        g.setColor(new Color(0x33, 0x99, 0xFF, 210));
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawPolyline(xs, ys, track.size());

        // Start marker (green)
        drawMarker(g, xs[0], ys[0], new Color(0x22, 0xBB, 0x44));
        // End marker (red)
        drawMarker(g, xs[track.size() - 1], ys[track.size() - 1], new Color(0xEE, 0x33, 0x33));

        g.dispose();
    }

    private void drawMarker(Graphics2D g, int x, int y, Color fill) {
        g.setColor(fill);
        g.fillOval(x - 8, y - 8, 16, 16);
        g.setStroke(new BasicStroke(2f));
        g.setColor(Color.WHITE);
        g.drawOval(x - 8, y - 8, 16, 16);
    }
}
