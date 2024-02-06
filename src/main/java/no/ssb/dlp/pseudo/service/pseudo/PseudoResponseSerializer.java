package no.ssb.dlp.pseudo.service.pseudo;

import io.reactivex.Flowable;

public class PseudoResponseSerializer {
    /**
     * Combine two <code>Flowable</code> JSON-objects (data and metadata) into a single <code>Flowable</code> that
     * represents the JSON-structure.
     * @param data a flowable of String elements, each in JSON-format
     * @param metadata a flowable of String elements, each in JSON-format
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
