package no.ssb.dlp.pseudo.service.mediatype;

import io.micronaut.core.naming.NameUtils;
import io.micronaut.http.MediaType;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.spi.FileTypeDetector;
import java.util.Optional;

public class MicronautFileTypeDetector extends FileTypeDetector {
    @Override
    public String probeContentType(Path path) throws IOException {
        Optional<MediaType> mediaType = MediaType.forExtension(NameUtils.extension(path.toFile().getName()));
        return mediaType.isPresent()
          ? mediaType.get().toString()
          : null;
    }
}