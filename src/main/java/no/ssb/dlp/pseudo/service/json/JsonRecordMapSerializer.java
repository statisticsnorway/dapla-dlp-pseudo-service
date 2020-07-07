package no.ssb.dlp.pseudo.service.json;

import no.ssb.dlp.pseudo.service.RecordMap;
import no.ssb.dlp.pseudo.service.RecordMapSerializer;
import no.ssb.dlp.pseudo.service.util.Json;

public class JsonRecordMapSerializer implements RecordMapSerializer<String> {

    @Override
    public String serialize(RecordMap record, int position) {
        return Json.from(record);
    }

}
