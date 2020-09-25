package no.ssb.dlp.pseudo.service.mediatype;

import io.micronaut.http.MediaType;
import lombok.Data;

@Data
public class Compression {
    private MediaType type;
    private CompressionEncryptionMethod encryption;
    private char[] password;

    public boolean isZipCompressionEnabled() {
        return MoreMediaTypes.APPLICATION_ZIP_TYPE.equals(type);
    }
}
