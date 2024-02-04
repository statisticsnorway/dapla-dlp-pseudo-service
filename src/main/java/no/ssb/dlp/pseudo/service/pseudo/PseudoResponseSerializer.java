package no.ssb.dlp.pseudo.service.pseudo;

import io.reactivex.Flowable;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class PseudoResponseSerializer {
    /**
     * Special case for serializing a Map of flowables. The key will represent the name and the value will contain
     * the contents, so that the resulting JSON will contain named arrays of data.
     */
    public static Flowable<String> serialize(Map<String, Flowable<String>> flowableMap) {
        /*
        return flowableMap.entrySet().stream()
                .flatMap(e -> e.getValue()
                        .map(s -> Flowable.just("\"", e.getKey(), "\": ").concatWith(e.getValue())));
        */
        AtomicBoolean first = new AtomicBoolean(true);
        return enclose(Flowable.concat(flowableMap.entrySet().stream().map(e -> {
                    if (first.getAndSet(false)) {
                        return Flowable.just("\"" + e.getKey() + "\": ").concatWith(e.getValue());
                    }
                    return Flowable.just(", \"" + e.getKey() + "\": ").concatWith(e.getValue());
                }
        ).toList()));
    }

    private static Flowable<String> enclose(Flowable<String> contents) {
        return Flowable.concat(Flowable.just("{"), contents, Flowable.just("}"));
    }

}
