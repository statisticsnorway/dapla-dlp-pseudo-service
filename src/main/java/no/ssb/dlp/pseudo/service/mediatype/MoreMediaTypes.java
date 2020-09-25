package no.ssb.dlp.pseudo.service.mediatype;

import io.micronaut.http.MediaType;


public class MoreMediaTypes {
    private MoreMediaTypes() {}

    public static final String APPLICATION_ZIP = "application/zip";
    public static final String TEXT_CSV = "text/csv";
    public static final MediaType APPLICATION_ZIP_TYPE = MediaType.of(APPLICATION_ZIP);
    public static final MediaType TEXT_CSV_TYPE = MediaType.of(TEXT_CSV);

    public static MediaType validContentType(MediaType contentType) {
        if (MoreMediaTypes.TEXT_CSV_TYPE.equals(contentType) || MediaType.APPLICATION_JSON_TYPE.equals(contentType)) {
            return contentType;
        } else if (MediaType.ALL_TYPE.equals(contentType)) {
            return MediaType.APPLICATION_JSON_TYPE;
        } else {
            throw new IllegalArgumentException("Unsupported media type: " + contentType);
        }
    }
}
