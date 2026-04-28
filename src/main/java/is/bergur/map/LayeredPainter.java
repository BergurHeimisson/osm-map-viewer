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
