package no.ssb.dlp.pseudo.service;

/**
 * @see no.ssb.dlp.pseudo.service.json.JsonRecordMapSerializer
 * @see no.ssb.dlp.pseudo.service.csv.CsvRecordMapSerializer
 */
public interface RecordMapSerializer<T> {

    /**
     * Serialize a RecordMap to some implementation specific format.
     *
     * @param record the RecordMap to serialize
     * @param position the record's sequence number
     * @return a serialized RecordMap
     */
    T serialize(RecordMap record, int position);

}
