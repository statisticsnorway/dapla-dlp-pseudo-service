package no.ssb.dlp.pseudo.service.pseudo;

import io.micronaut.http.MediaType;
import lombok.RequiredArgsConstructor;
import no.ssb.dlp.pseudo.core.FieldPseudonymizer;
import no.ssb.dlp.pseudo.core.PseudoFuncRule;
import no.ssb.dlp.pseudo.core.StreamPseudonymizer;
import no.ssb.dlp.pseudo.core.json.JsonStreamPseudonymizer;
import no.ssb.dlp.pseudo.core.map.RecordMapPseudonymizer;

import javax.inject.Singleton;
import java.util.Collection;
import java.util.Optional;

@RequiredArgsConstructor
@Singleton
public class PseudonymizerFactory {
    private final PseudoSecrets pseudoSecrets;

    public FieldPseudonymizer newFieldPseudonymizer(Collection<PseudoFuncRule> rules) {
        return new FieldPseudonymizer.Builder()
          .secrets(pseudoSecrets.resolve())
          .rules(rules)
          .build();
    }

    public RecordMapPseudonymizer newRecordMapPseudonymizer(Collection<PseudoFuncRule> rules) {
        FieldPseudonymizer fieldPseudonymizer = newFieldPseudonymizer(rules);
        return new RecordMapPseudonymizer(fieldPseudonymizer);
    }

    public JsonStreamPseudonymizer newJsonStreamPseudonymizer(Collection<PseudoFuncRule> rules) {
        RecordMapPseudonymizer recordMapPseudonymizer = newRecordMapPseudonymizer(rules);
        return new JsonStreamPseudonymizer(recordMapPseudonymizer);
    }

    public StreamPseudonymizer newStreamPseudonymizer(Collection<PseudoFuncRule> rules, MediaType mediaType) {
        mediaType = Optional.ofNullable(mediaType).orElse(MediaType.APPLICATION_JSON_TYPE);
        if (MediaType.APPLICATION_JSON.equals(mediaType.toString())) {
            return newJsonStreamPseudonymizer(rules);
        }
        throw new IllegalArgumentException("No StreamPseudonymizer found for media type " + mediaType);
    }

}
