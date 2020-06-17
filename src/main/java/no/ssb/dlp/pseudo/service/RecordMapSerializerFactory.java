package no.ssb.dlp.pseudo.service;

import io.micronaut.http.MediaType;
import no.ssb.dlp.pseudo.service.csv.CsvRecordMapSerializer;
import no.ssb.dlp.pseudo.service.json.JsonRecordMapSerializer;

public class RecordMapSerializerFactory {

    public static RecordMapSerializer<String> newFromMediaType(MediaType mediaType) {
        if (MediaType.APPLICATION_JSON_TYPE.equals(mediaType)) {
            return new JsonRecordMapSerializer();
        }
        if (MoreMediaTypes.TEXT_CSV_TYPE.equals(mediaType)) {
            return new CsvRecordMapSerializer();
        }
        else {
            throw new IllegalArgumentException("Unsupported media type - no RecordMapSerializer implementation exists for " + mediaType);
        }
    }

}
