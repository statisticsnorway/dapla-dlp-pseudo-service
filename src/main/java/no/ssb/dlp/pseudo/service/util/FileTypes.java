package no.ssb.dlp.pseudo.service.util;

import io.micronaut.http.MediaType;
import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

@UtilityClass
public class FileTypes {

    public static Optional<MediaType> determineFileType(File file) {
        try {
            if (!file.exists()) {
                throw new IOException("File not found: " + file);
            }
            String contentType = Files.probeContentType(file.toPath());
            return contentType == null
              ? Optional.empty()
              : Optional.of(MediaType.of(contentType));
        }
        catch (IOException e) {
            return Optional.empty();
        }
    }
}
