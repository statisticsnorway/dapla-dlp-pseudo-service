package no.ssb.dlp.pseudo.service.csv;

import com.google.common.base.Joiner;
import no.ssb.dlp.pseudo.service.MapTraverser;
import no.ssb.dlp.pseudo.service.RecordMap;
import no.ssb.dlp.pseudo.service.RecordMapSerializer;

import java.util.ArrayList;
import java.util.List;

public class CsvRecordMapSerializer implements RecordMapSerializer<String> {

    private List<String> headers = new ArrayList<>();
    private static final char SEPARATOR = ';';
    private static final Joiner JOINER = Joiner.on(SEPARATOR).useForNull("null");

    // This implementation is a bit so-so - deducing header stuff from only one record.
    @Override
    public String serialize(RecordMap record, int position) {
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


    public static class CsvSerializationException extends RuntimeException {
        public CsvSerializationException(String message) {
            super(message);
        }
    }
}
