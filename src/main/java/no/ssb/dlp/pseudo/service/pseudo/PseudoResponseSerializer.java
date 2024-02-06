package no.ssb.dlp.pseudo.service.pseudo;

import io.reactivex.Flowable;

public class PseudoResponseSerializer {
    /**
     * Special case for serializing a Map of flowables. The key will represent the name and the value will contain
     * the contents, so that the resulting JSON will contain named arrays of data.
     */
    public static Flowable<String> serialize(Flowable<String> data, Flowable<String> metadata) {
        return enclose(data.concatMap(item -> Flowable.just(item, ","))
                .startWith("\"data\": [")
                .skipLast(1) // Skip last comma
                .concatWith(Flowable.just("], \"metadata\": ["))
                .concatWith(metadata.concatMap(item -> Flowable.just(item, ",")).skipLast(1))
                .concatWith(Flowable.just("]")));
    }

    private static Flowable<String> enclose(Flowable<String> contents) {
        return Flowable.concat(Flowable.just("{"), contents, Flowable.just("}"));
    }

}
