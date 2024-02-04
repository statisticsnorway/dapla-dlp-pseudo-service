package no.ssb.dlp.pseudo.service.pseudo;

import com.google.common.collect.Lists;
import io.reactivex.Flowable;
import no.ssb.dlp.pseudo.core.util.Json;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.util.List;
import java.util.Map;

class PseudoResponseSerializerTest {

    @Test
    void testSerializeMap() throws JSONException {
        Flowable<String> data = Flowable.just(Json.from(List.of(
                Map.of("person", Map.of("name", Map.of("firstName", "Donald", "lastName", "Duck"))),
                Map.of("person", Map.of("name", Map.of("firstName", "Bolla", "lastName", "Bollerud")))
        )));
        Flowable<String> metadata = Flowable.just(
                Json.from(Map.of("variable_descriptions", Map.of("name", Map.of("type", "string", "description", "name variable"))))
        );
        String got = String.join("", Lists.newArrayList(PseudoResponseSerializer.serialize(Map.of("data", data, "metadata", metadata)).blockingIterable()));
        String want = """
                {
                  "metadata": {
                    "variable_descriptions": {
                      "name": {
                        "description": "name variable",
                        "type": "string"
                      }
                    }
                  },
                  "data": [
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
                }
                """;
        System.out.println(got);
        JSONAssert.assertEquals(want, got, JSONCompareMode.STRICT);
    }}