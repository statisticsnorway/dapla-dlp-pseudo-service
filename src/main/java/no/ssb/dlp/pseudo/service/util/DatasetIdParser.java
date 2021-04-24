package no.ssb.dlp.pseudo.service.util;

import no.ssb.dapla.dataset.api.DatasetId;

public class DatasetIdParser {

    /**
     * <p>Parse creates a DatasetId from a dataset path String, like: `/path/to/dataset`</p>
     *
     * <p>The path can include the dataset version using a special `@` notation (aka "short format"),
     * like so: `/path/to/dataset@1619242421337`
     * </p>
     *
     * @param path dataset path with or without version
     * @return
     * @throws ParseException
     */
    public static DatasetId parse(String path) throws ParseException {
        if (path == null) {
            throw new ParseException("dataset path cannot be null");
        }

        String version = "";
        int versionPos = path.indexOf("@");
        if (versionPos != -1) {
            version = path.substring(versionPos+1).trim();
            path = path.substring(0, versionPos);
        }

        if (version.isEmpty()) {
            version = "" + System.currentTimeMillis();
        }

        path = path.trim();
        if (path.isEmpty()) {
            throw new ParseException("dataset path cannot be empty");
        }

        return DatasetId.newBuilder()
          .setPath(path)
          .setVersion(version)
          .build();
    }

    public static class ParseException extends RuntimeException {
        public ParseException(String message) {
            super(message);
        }
    }
}
