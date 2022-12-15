package no.ssb.dlp.pseudo.service.pseudo;

import io.micronaut.http.MediaType;
import lombok.RequiredArgsConstructor;
import no.ssb.dlp.pseudo.core.PseudoFuncRule;
import no.ssb.dlp.pseudo.core.PseudoKeyset;
import no.ssb.dlp.pseudo.core.StreamPseudonymizer;
import no.ssb.dlp.pseudo.core.csv.CsvStreamPseudonymizer;
import no.ssb.dlp.pseudo.core.field.FieldPseudonymizer;
import no.ssb.dlp.pseudo.core.file.MoreMediaTypes;
import no.ssb.dlp.pseudo.core.json.JsonStreamPseudonymizer;
import no.ssb.dlp.pseudo.core.map.RecordMapPseudonymizer;

import javax.inject.Singleton;
import java.util.Collection;
import java.util.Optional;

@RequiredArgsConstructor
@Singleton
public class PseudonymizerFactory {
    private final PseudoSecrets pseudoSecrets;

    public FieldPseudonymizer newFieldPseudonymizer(Collection<PseudoFuncRule> rules, Collection<PseudoKeyset> keysets) {
        return new FieldPseudonymizer.Builder()
          .secrets(pseudoSecrets.resolve())
          .rules(rules)
          .keysets(keysets)
          .build();
    }

    public RecordMapPseudonymizer newRecordMapPseudonymizer(Collection<PseudoFuncRule> rules, Collection<PseudoKeyset> keysets) {
        FieldPseudonymizer fieldPseudonymizer = newFieldPseudonymizer(rules, keysets);
        return new RecordMapPseudonymizer(fieldPseudonymizer);
    }

    public JsonStreamPseudonymizer newJsonStreamPseudonymizer(Collection<PseudoFuncRule> rules, Collection<PseudoKeyset> keysets) {
        RecordMapPseudonymizer recordMapPseudonymizer = newRecordMapPseudonymizer(rules, keysets);
        return new JsonStreamPseudonymizer(recordMapPseudonymizer);
    }

    public CsvStreamPseudonymizer newCsvStreamPseudonymizer(Collection<PseudoFuncRule> rules, Collection<PseudoKeyset> keysets) {
        RecordMapPseudonymizer recordMapPseudonymizer = newRecordMapPseudonymizer(rules, keysets);
        return new CsvStreamPseudonymizer(recordMapPseudonymizer);
    }

    public StreamPseudonymizer newStreamPseudonymizer(Collection<PseudoFuncRule> rules, Collection<PseudoKeyset> keysets, MediaType mediaType) {
        mediaType = Optional.ofNullable(mediaType).orElse(MediaType.APPLICATION_JSON_TYPE);
        if (MediaType.APPLICATION_JSON.equals(mediaType.toString())) {
            return newJsonStreamPseudonymizer(rules, keysets);
        }
        else if (MoreMediaTypes.TEXT_CSV.equals(mediaType.toString())) {
            return newCsvStreamPseudonymizer(rules, keysets);
        }

        throw new IllegalArgumentException("No StreamPseudonymizer found for media type " + mediaType);
    }

}
