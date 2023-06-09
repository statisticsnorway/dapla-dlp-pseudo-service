package no.ssb.dlp.pseudo.service.pseudo;

import lombok.*;
import no.ssb.dlp.pseudo.core.func.PseudoFuncRule;
import no.ssb.dlp.pseudo.core.tink.model.EncryptedKeysetWrapper;
/**
 * Represents a field to be pseudonymized.
 */
@Data
public class PseudoField {

    private String name;

    private String value;

    private PseudoConfig pseudoConfig;

    /**
     * Constructs a {@code PseudoField} object with the specified name, value, keyset, pseudoConfig. If no keyset is supplied
     * a default pseudo configuration is used.
     *
     * @param name                      The name of the field.
     * @param value                     The value of the field.
     * @param keyset                    The encrypted keyset to be used for pseudonymization.
     * @param defaultFieldPseudoConfig  The default field pseudo configuration.
     */
    public PseudoField(String name, String value, EncryptedKeysetWrapper keyset, DefaultFieldPseudoConfig defaultFieldPseudoConfig) {
        this.name = name;
        this.value = value;
        this.pseudoConfig = new PseudoConfig();

        if (keyset == null) {
            this.pseudoConfig = defaultFieldPseudoConfig.getDefaultPseudoConfig();

        } else {
            this.pseudoConfig.getRules().add(new PseudoFuncRule(PseudoField.class.getSimpleName(), "**",
                    String.format("daead(keyId=%s)",
                            keyset.getKeysetInfo().getPrimaryKeyId())));
            this.pseudoConfig.getKeysets().add(keyset);
        }
    }

    /**
     * Creates a {@link ResponsePseudoField} object with the provided encrypted value.
     *
     * @param encryptedValue The encrypted value of the field.
     * @return A {@link ResponsePseudoField} object containing the encrypted value, field name, and pseudo rules.
     */
    public ResponsePseudoField getResponseField(String encryptedValue) {
        return new ResponsePseudoField(encryptedValue, name, pseudoConfig);
    }
}

@Data
@AllArgsConstructor
class ResponsePseudoField {
    private String value;
    private String fieldName;
    private PseudoConfig pseudoRules;
}
