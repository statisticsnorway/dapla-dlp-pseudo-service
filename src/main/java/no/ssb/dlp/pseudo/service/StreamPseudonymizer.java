package no.ssb.dlp.pseudo.service;

import io.reactivex.Flowable;

import java.io.InputStream;

public interface StreamPseudonymizer {
    <T> Flowable<T> pseudonymize(InputStream is, RecordMapSerializer<T> serializer);
    <T> Flowable<T> depseudonymize(InputStream is, RecordMapSerializer<T> serializer);
}
