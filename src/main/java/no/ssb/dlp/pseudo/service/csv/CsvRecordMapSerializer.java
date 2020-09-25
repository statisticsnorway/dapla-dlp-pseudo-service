package no.ssb.dlp.pseudo.service.csv;

import com.google.common.base.Joiner;
import io.reactivex.Flowable;
import no.ssb.dlp.pseudo.service.MapTraverser;
import no.ssb.dlp.pseudo.service.RecordMapSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CsvRecordMapSerializer implements RecordMapSerializer<String> {

    private List<String> headers = new ArrayList<>();
    private static final char SEPARATOR = ';';
    private static final Joiner JOINER = Joiner.on(SEPARATOR).useForNull("null");

    // This implementation is a bit so-so - deducing header stuff from only one record.
    @Override
    public String serialize(Map<String, Object> record, int position) {
        boolean recordHeaders = headers.isEmpty();
        boolean printHeaders = position == 0;
        List<String> values = new ArrayList<>();
        MapTraverser.traverse(record, (field, value) -> {
            if (recordHeaders) {
                headers.add(field.getName());
            }
            values.add(value);
            return null;
        });

        if (values.size() != headers.size()) {
            throw new CsvSerializationException("CSV value to header mismatch for record at pos=" + position +
              ". Expected CSV row to have " + headers.size() + " columns, but encountered " + values.size() +
              ". This can happen if the source document does not contain values for all fields.");
        }

        return (printHeaders ?
          JOINER.join(headers) + System.lineSeparator() : "") +
          JOINER.join(values) + System.lineSeparator();
    }

    @Override
    public Flowable<String> serialize(Flowable<Map<String, Object>> recordStream) {
        throw new RuntimeException("Not implemented yet!");
    }


    public static class CsvSerializationException extends RuntimeException {
        public CsvSerializationException(String message) {
            super(message);
        }
    }
}
