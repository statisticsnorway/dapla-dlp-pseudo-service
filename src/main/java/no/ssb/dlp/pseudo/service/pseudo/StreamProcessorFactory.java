package no.ssb.dlp.pseudo.service.pseudo;

import io.micronaut.http.MediaType;
import lombok.RequiredArgsConstructor;
import no.ssb.dlp.pseudo.core.StreamProcessor;
import no.ssb.dlp.pseudo.core.file.MoreMediaTypes;
import no.ssb.dlp.pseudo.core.json.JsonStreamProcessor;
import no.ssb.dlp.pseudo.core.map.RecordMapProcessor;

import javax.inject.Singleton;

@RequiredArgsConstructor
@Singleton
public class StreamProcessorFactory {

    public StreamProcessor newStreamProcessor(MediaType contentType, RecordMapProcessor recordMapProcessor) {
        if (MediaType.APPLICATION_JSON.equals(contentType.toString())) {
            return new JsonStreamProcessor(recordMapProcessor);
        }
        else if (MoreMediaTypes.TEXT_CSV.equals(contentType.toString())) {
            //return new CsvStreamProcessor(recordMapProcessor);
            throw new UnsupportedOperationException("Not yet implemented: CsvStreamProcessor");
        }

        throw new IllegalArgumentException("No StreamProcessor found for content type " + contentType);
    }

}
