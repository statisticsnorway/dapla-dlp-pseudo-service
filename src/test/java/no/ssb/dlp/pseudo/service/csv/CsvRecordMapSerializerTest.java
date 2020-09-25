package no.ssb.dlp.pseudo.service.csv;

import com.fasterxml.jackson.core.type.TypeReference;
import no.ssb.dlp.pseudo.service.RecordMap;
import no.ssb.dlp.pseudo.service.util.Json;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvRecordMapSerializerTest {

    private CsvRecordMapSerializer serializer;

    @BeforeEach
    public void setup() {
        serializer = new CsvRecordMapSerializer();
    }

    public String serializeHeader(Map<String, Object> recordMap) {
        return serializer.serialize(recordMap, 0).split("\\n")[0];
    }

    public String serialize(Map<String, Object> recordMap) {
        return serializer.serialize(recordMap, 1);
    }

    @Test
    void simpleRecordMap_serialize_shouldCreateCsvLine() {
        Map<String, Object> r = Json.toObject(RecordMap.class, """
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
        Map<String, Object> r = Json.toObject(RecordMap.class, """
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

    @Test
    void recordMapWithMismatchValueCount_serialize_shouldThrowException() {
        List<RecordMap> items = Json.toObject(new TypeReference<>() {}, """
        [        
            {
                "someString": "Foo",
                "someBoolean": true
            },
            {
                "someString": "Bar"
            }
        ]
        """);

        assertThatThrownBy(() -> {
              for (int i=0; i<items.size(); i++) {
                  serializer.serialize(items.get(i), i);
              }
        })
          .isInstanceOf(CsvRecordMapSerializer.CsvSerializationException.class)
          .hasStackTraceContaining("CSV value to header mismatch for record at pos=1");
    }
}
