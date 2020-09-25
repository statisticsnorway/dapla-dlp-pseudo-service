package no.ssb.dlp.pseudo.service.json;

import com.google.common.collect.Lists;
import io.reactivex.Flowable;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JsonRecordMapSerializerTest {

    @Test
    void thatSerializeFlowableWorks() throws JSONException {
        Flowable<Map<String, Object>> records = Flowable.fromArray(
                Map.of("person", Map.of("name", Map.of("firstName", "Donald", "lastName", "Duck"))),
                Map.of("person", Map.of("name", Map.of("firstName", "Bolla", "lastName", "Bollerud")))
        );

        String want = """
                [
                    {
                        "person": {
                            "name": {
                                "firstName": "Donald",
                                "lastName": "Duck"
                            }
                        }
                    },
                    {
                        "person": {
                            "name": {
                                "firstName": "Bolla",
                                "lastName": "Bollerud"
                            }
                        }
                    }
                ]
                """;

        String got = String.join("", Lists.newArrayList(new JsonRecordMapSerializer().serialize(records).blockingIterable()));
        JSONAssert.assertEquals(want, got, JSONCompareMode.STRICT);
    }
}
