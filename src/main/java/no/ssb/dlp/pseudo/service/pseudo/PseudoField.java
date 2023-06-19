package no.ssb.dlp.pseudo.service.pseudo;

import lombok.*;
import lombok.experimental.SuperBuilder;
import no.ssb.dlp.pseudo.core.func.PseudoFuncRule;
import no.ssb.dlp.pseudo.core.tink.model.EncryptedKeysetWrapper;

import java.util.List;

/**
 * Represents a field to be pseudonymized.
 */
public class PseudoField extends AbstractPseudoField<ResponsePseudoField> {
    @Getter(AccessLevel.PROTECTED)
    private static final String DEFAULT_PSEUDO_FUNC = "daead(keyId=ssb-common-key-1)";

    /**
     * Constructs a {@code PseudoField} object with the specified name, values, keyset, pseudoConfig. If no keyset is supplied
     * a default pseudo configuration is used.
     *
     * @param name   The name of the field.
     * @param values The values of the field.
     * @param pseudoFunc The pseudo function definition.
     * @param keyset The encrypted keyset to be used for pseudonymization.
     */
    public PseudoField(String name, List<String> values, String pseudoFunc, EncryptedKeysetWrapper keyset) {
        super(name, values);

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
