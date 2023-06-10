package no.ssb.dlp.pseudo.service.pseudo;

import io.micronaut.http.annotation.Get;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import no.ssb.dlp.pseudo.core.func.PseudoFuncRule;
import no.ssb.dlp.pseudo.core.tink.model.EncryptedKeysetWrapper;
import no.ssb.dlp.pseudo.service.sid.SidMapper;
import no.ssb.dlp.pseudo.service.sid.SidService;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a SID field to be pseudonymized.
 */
@Getter
@Setter
public class PseudoSIDField extends AbstractPseudoField<ResponsePseudoSIDField> {
    @Getter(AccessLevel.PROTECTED)
    private static final PseudoConfig DEFAULT_SID_PSEUDO_CONFIG = new PseudoConfig(new PseudoFuncRule("default-sid", "**",
            "ff31(keyId=papis-key-1, strategy=SKIP)"));
    private String stableIDSnapshot;

    private List<String> sidValues;

    /**
     * Constructs a {@code PseudoSIDField} object with the specified name, values, keyset, and pseudoConfig.
     * If no keyset is supplied the default SID field pseudo configuration is used.
     *
     * @param name   The name of the SID field.
     * @param values The values of the SID field.
     * @param keyset The encrypted keyset to be used for pseudonymization.
     */
    public PseudoSIDField(String name, List<String> values, EncryptedKeysetWrapper keyset) {
        super(name, values);

        if (keyset == null) {
            pseudoConfig = DEFAULT_SID_PSEUDO_CONFIG;

        } else {
            pseudoConfig = new PseudoConfig();
            pseudoConfig.getRules().add(new PseudoFuncRule(PseudoSIDField.class.getName(), "**",
                    String.format("ff31(keyId=%s, strategy=SKIP)",
                            keyset.getKeysetInfo().getPrimaryKeyId())));
            pseudoConfig.getKeysets().add(keyset);
        }
    }

    /**
     * Maps the field values to SID using the provided {@link SidService}.
     *
     * @param sidMapper The {@link SidService} used to look up and map the values to a SID.
     */
    public void mapValueToSid(SidMapper sidMapper) {
        /*
        TODO: Get and Set snapshot version
        this.stableIDSnapshot = sidInfo.getCurrentStabelIDSnapshot();
        */
        sidValues = new ArrayList<>();

        values.stream()
                .map(value -> (String) sidMapper.map(value))
                .forEach(result -> sidValues.add(result));
    }

    /**
     * Pseudonymizes the SID field using the provided record processor factory and returns a {@link ResponsePseudoSIDField} object
     * containing the encrypted values, field name, pseudo rules, and stable ID snapshot.
     *
     * @param recordProcessorFactory The record processor factory used to create a field pseudonymizer.
     * @return A {@link ResponsePseudoSIDField} object containing the encrypted values, field name, pseudo rules, and stable ID snapshot.
     */
    public ResponsePseudoSIDField pseudonymizeThenGetResponseField(RecordMapProcessorFactory recordProcessorFactory) {
        List<String> encryptedValues = pseudonymize(sidValues, recordProcessorFactory);
        return this.getResponseField(encryptedValues);
    }


    /**
     * Creates a {@link ResponsePseudoSIDField} object with the provided encrypted values.
     *
     * @param encryptedValues The encrypted values of the SID field.
     * @return A {@link ResponsePseudoSIDField} object containing the encrypted values, field name, pseudo rules, and stable ID snapshot.
     */
    public ResponsePseudoSIDField getResponseField(List<String> encryptedValues) {
        return ResponsePseudoSIDField.builder()
                .values(encryptedValues)
                .fieldName(name)
                .pseudoRules(pseudoConfig)
                .stableIDSnapshot(stableIDSnapshot).build();
    }

}

@SuperBuilder
class ResponsePseudoSIDField extends ResponsePseudoField {
    private String stableIDSnapshot;
}