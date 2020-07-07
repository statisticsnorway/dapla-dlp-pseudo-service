package no.ssb.dlp.pseudo.service.csv;

import no.ssb.dlp.pseudo.service.RecordMap;
import no.ssb.dlp.pseudo.service.util.Json;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CsvRecordMapSerializerTest {

    private CsvRecordMapSerializer serializer;

    @BeforeEach
    public void setup() {
        serializer = new CsvRecordMapSerializer();
    }

    public String serializeHeader(RecordMap recordMap) {
        return serializer.serialize(recordMap, 0).split("\\n")[0];
    }

    public String serialize(RecordMap recordMap) {
        return serializer.serialize(recordMap, 1);
    }

    @Test
    void simpleRecordMap_serialize_shouldCreateCsvLine() {
        RecordMap r = Json.toObject(RecordMap.class, """
        {
            "navn": "Bolla",
            "alder": 42,
            "adresse": {
                "adresselinjer": ["Bolleveien 1", "Bollerud"],
                "postnummer": "0123",
                "poststed": "Oslo"
            }
        }
        """);

        assertThat(serializeHeader(r)).isEqualTo("navn;alder;adresselinjer[0];adresselinjer[1];postnummer;poststed");
        assertThat(serialize(r)).isEqualTo("Bolla;42;Bolleveien 1;Bollerud;0123;Oslo\n");
    }

    @Test
    void recordMapWithNullAndEmptyVals_serialize_shouldCreateCsvLine() {
        RecordMap r = Json.toObject(RecordMap.class, """
        {
            "someString": "Foo",
            "someEmptyString": "",
            "someBlankString": "          ",
            "someInt": 42,
            "someNullValue": null,
            "someBoolean": true
        }
        """);

        assertThat(serialize(r)).isEqualTo("Foo;;          ;42;null;true\n");
    }


}