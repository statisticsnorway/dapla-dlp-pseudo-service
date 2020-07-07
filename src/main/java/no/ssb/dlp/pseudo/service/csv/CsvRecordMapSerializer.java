package no.ssb.dlp.pseudo.service.csv;

import com.google.common.base.Joiner;
import no.ssb.dlp.pseudo.service.MapTraverser;
import no.ssb.dlp.pseudo.service.RecordMap;
import no.ssb.dlp.pseudo.service.RecordMapSerializer;

import java.util.ArrayList;
import java.util.List;

public class CsvRecordMapSerializer implements RecordMapSerializer<String> {

    private static char SEPARATOR = ';';
    private static Joiner JOINER = Joiner.on(SEPARATOR).useForNull("null");

    // FIXME: This implementation is a bit so-so - deducing header stuff from only the first record.
    @Override
    public String serialize(RecordMap record, int position) {
        boolean renderHeader = (position == 0);
        List<String> headers = new ArrayList<>();
        List<String> values = new ArrayList<>();
        MapTraverser.traverse(record, (field, value) -> {
            headers.add(field.getName());
            values.add(value);
            return null;
        });

        return (renderHeader ?
          JOINER.join(headers) + System.lineSeparator() : "") +
          JOINER.join(values) + System.lineSeparator();
    }
}
