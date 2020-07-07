package no.ssb.dlp.pseudo.service.mediatype;

import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaMetadataKeys;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AutoDetectParser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.spi.FileTypeDetector;

public class TikaLiteFileTypeDetector extends FileTypeDetector {

    @Override
    public String probeContentType(Path path) throws IOException {
        try (final InputStream is = Files.newInputStream(path)) {
            AutoDetectParser parser = new AutoDetectParser();
            Detector detector = parser.getDetector();
            Metadata md = new Metadata();
            md.add(TikaMetadataKeys.RESOURCE_NAME_KEY, path.toFile().getName());
            MediaType mediaType = detector.detect(is, md);
            return mediaType.toString();
        } catch (Exception ignored) {
            return null;
        }
    }
}
