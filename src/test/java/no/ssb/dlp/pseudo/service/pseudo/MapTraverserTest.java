package no.ssb.dlp.pseudo.service.pseudo;

import io.micronaut.test.annotation.MicronautTest;
import no.ssb.dlp.pseudo.service.MapTraverser;
import no.ssb.dlp.pseudo.service.util.Json;
import org.junit.jupiter.api.Test;

import java.util.Map;

@MicronautTest
class MapTraverserTest {

    @Test
    public void testTraverse() {
        Map<String, Object> sourceData = Json.toGenericMap("""
        {
            "navn": "Bolla",
            "alder": 42,
            "adresse": {
                "adresselinjer": ["Bolleveien 1", "Bollerud"],
                "postnummer": "0123",
                "poststed": "Oslo",
                "land": null
            }
        }
        """);
        MapTraverser.traverse(sourceData, (field, value) -> {
            System.out.println(field.getPath() + " -> " + value);
            return null;
          }
        );
    }

}