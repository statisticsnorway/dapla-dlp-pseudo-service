package no.ssb.dlp.pseudo.service;

public interface RecordMapSerializer<T> {
    T serialize(RecordMap record, int position);
}
