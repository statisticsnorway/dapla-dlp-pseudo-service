package no.ssb.dlp.pseudo.service.json;

import io.reactivex.Flowable;
import io.reactivex.Single;
import no.ssb.dlp.pseudo.service.RecordMapSerializer;
import no.ssb.dlp.pseudo.service.util.Json;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class JsonRecordMapSerializer implements RecordMapSerializer<String> {

    @Override
    public String serialize(Map<String, Object> record, int position) {
        return Json.from(record);
    }

    @Override
    public Flowable<String> serialize(Flowable<Map<String, Object>> recordStream) {
        AtomicBoolean first = new AtomicBoolean(true);
        return recordStream.map(record -> {
            if (first.getAndSet(false)) {
                return "[%s".formatted(serialize(record, -1));
            }
            return ",%s".formatted(serialize(record, -1));
        }).concatWith(Single.just("]"));
    }
}
