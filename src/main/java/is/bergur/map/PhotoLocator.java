package is.bergur.map;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.lang.GeoLocation;
import org.jxmapviewer.viewer.GeoPosition;

import java.io.File;
import java.util.Optional;

public class PhotoLocator {

    public static Optional<GeoPosition> readGps(File photo) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(photo);
            GpsDirectory gps = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            if (gps == null) return Optional.empty();
            GeoLocation loc = gps.getGeoLocation();
            if (loc == null || loc.isZero()) return Optional.empty();
            return Optional.of(new GeoPosition(loc.getLatitude(), loc.getLongitude()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
