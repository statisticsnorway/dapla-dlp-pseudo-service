package no.ssb.dlp.pseudo.service.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.reactivex.Emitter;
import io.reactivex.Flowable;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import no.ssb.dlp.pseudo.service.PseudoOperation;
import no.ssb.dlp.pseudo.service.RecordMap;
import no.ssb.dlp.pseudo.service.RecordMapPseudonymizer;
import no.ssb.dlp.pseudo.service.RecordMapSerializer;
import no.ssb.dlp.pseudo.service.StreamPseudonymizer;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
@Slf4j
public class JsonStreamPseudonymizer implements StreamPseudonymizer {

    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
        OBJECT_MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    private final RecordMapPseudonymizer recordPseudonymizer;

    @Override
    public <T> Flowable<T> pseudonymize(InputStream is, RecordMapSerializer<T> serializer) {
        return processStream(PseudoOperation.PSEUDONYMIZE, is, serializer);
    }

    @Override
    public <T> Flowable<T> depseudonymize(InputStream is, RecordMapSerializer<T> serializer) {
        return processStream(PseudoOperation.DEPSEUDONYMIZE, is, serializer);
    }

    <T> JsonProcessorContext<T> initJsonProcessorContext(PseudoOperation operation, InputStream is, RecordMapSerializer<T> serializer) throws IOException {
        final JsonParser jsonParser = OBJECT_MAPPER.getFactory().createParser(is);
        if (jsonParser.nextToken() != JsonToken.START_ARRAY) {
            throw new IllegalStateException("Expected content to be a json array");
        }

        JsonProcessorContext ctx = new JsonProcessorContext(operation, jsonParser, serializer);
        return ctx;
    }

    private <T> Flowable<T> processStream(PseudoOperation operation, InputStream is, RecordMapSerializer<T> serializer) {
        return Flowable.generate(
          () -> initJsonProcessorContext(operation, is, serializer),
          (ctx, emitter) -> {this.processItem(ctx, emitter);},
          JsonProcessorContext::close
        );
    }

    private <T> void processItem(JsonProcessorContext<T> ctx, Emitter<T> emitter) throws IOException {
        JsonParser jsonParser = ctx.getJsonParser();
        if (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            int position = ctx.currentPosition.getAndIncrement();
            RecordMap record = OBJECT_MAPPER.readValue(jsonParser, RecordMap.class);
            RecordMap processedRecord = ctx.operation == PseudoOperation.PSEUDONYMIZE
              ? recordPseudonymizer.pseudonymize(record)
              : recordPseudonymizer.depseudonymize(record);
            emitter.onNext(ctx.getSerializer().serialize(processedRecord, position));
        } else {
            emitter.onComplete();
        }
    }

    @Value
    static class JsonProcessorContext<T> {
        private final PseudoOperation operation;
        private final JsonParser jsonParser;
        private final RecordMapSerializer<T> serializer;
        private final AtomicInteger currentPosition = new AtomicInteger();

        public void close() throws IOException {
            jsonParser.close();
        }
    }

}
