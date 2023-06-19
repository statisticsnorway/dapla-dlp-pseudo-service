package no.ssb.dlp.pseudo.service.pseudo;

import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
import no.ssb.dlp.pseudo.core.field.FieldDescriptor;
import no.ssb.dlp.pseudo.core.field.FieldPseudonymizer;
import no.ssb.dlp.pseudo.core.func.PseudoFuncRule;
import no.ssb.dlp.pseudo.core.tink.model.EncryptedKeysetWrapper;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a field to be pseudonymized.
 */
@Log4j2
@Data
public class PseudoField {
    @Getter(AccessLevel.PROTECTED)
    private static final String DEFAULT_PSEUDO_FUNC = "daead(keyId=ssb-common-key-1)";

    protected String name;
    protected List<String> values;
    protected PseudoConfig pseudoConfig;
    private List<String> sidValues;

    /**
     * Constructs a {@code PseudoField} object with the specified name, values, keyset, pseudoConfig. If no keyset is supplied
     * a default pseudo configuration is used.
     *
     * @param name       The name of the field.
     * @param values     The values of the field.
     * @param pseudoFunc The pseudo function definition.
     * @param keyset     The encrypted keyset to be used for pseudonymization.
     */
    public PseudoField(String name, List<String> values, String pseudoFunc, EncryptedKeysetWrapper keyset) {
        this.name = name;
        this.values = values;

        pseudoConfig = new PseudoConfig();

        if (pseudoFunc == null) {
            pseudoFunc = DEFAULT_PSEUDO_FUNC;
        }
        if (keyset != null) {
            pseudoConfig.getKeysets().add(keyset);
        }
        pseudoConfig.getRules().add(new PseudoFuncRule(name, "**", pseudoFunc));
    }

    /**
     * Pseudonymizes the field using the provided record processor factory and returns a {@link ResponsePseudoField} object
     * containing the encrypted values, field name, and pseudo rules.
     *
     * @param recordProcessorFactory The record processor factory used to create a field pseudonymizer.
     * @return A {@link ResponsePseudoField} object containing the encrypted values, field name, and pseudo rules.
     */
    public ResponsePseudoField pseudonymizeThenGetResponseField(RecordMapProcessorFactory recordProcessorFactory) {
        List<String> encryptedValues = pseudonymize(values, recordProcessorFactory);
        return this.getResponseField(encryptedValues);
    }


    protected List<String> pseudonymize(List<String> values, RecordMapProcessorFactory recordProcessorFactory) {
        Instant startTime = Instant.now();

        FieldPseudonymizer fieldPseudonymizer = recordProcessorFactory.newFieldPseudonymizer(this.getPseudoConfig().getRules(), RecordMapProcessorFactory.pseudoKeysetsOf(this.getPseudoConfig().getKeysets()));

        ArrayList<String> encryptedValues = new ArrayList<>();

        values.stream().map(value -> fieldPseudonymizer.pseudonymize(new FieldDescriptor(this.getName()), value)).forEach(result -> encryptedValues.add(result));

        Instant endTime = Instant.now();
        Duration duration = Duration.between(startTime, endTime);
        log.info("Pseudonymizing field '{}' took {} milliseconds.", this.getName(), duration.toMillis());

        return encryptedValues;
    }


    /**
     * Creates a {@link ResponsePseudoField} object with the provided encrypted value.
     *
     * @param encryptedValues The encrypted values of the field.
     * @return A {@link ResponsePseudoField} object containing the encrypted values, field name, and pseudo rules.
     */
    public ResponsePseudoField getResponseField(List<String> encryptedValues) {
        return ResponsePseudoField.builder()
                .values(encryptedValues)
                .fieldName(name)
                .pseudoRules(pseudoConfig).build();
    }
}

@Data
@SuperBuilder
class ResponsePseudoField {
    private List<String> values;
    private String fieldName;
    private PseudoConfig pseudoRules;
}
