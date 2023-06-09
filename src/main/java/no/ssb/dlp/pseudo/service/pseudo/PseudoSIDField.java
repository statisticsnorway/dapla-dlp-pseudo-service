package no.ssb.dlp.pseudo.service.pseudo;

import com.google.common.base.Strings;
import io.reactivex.Single;
import lombok.AllArgsConstructor;
import lombok.Data;
import no.ssb.dlp.pseudo.core.func.PseudoFuncRule;
import no.ssb.dlp.pseudo.core.tink.model.EncryptedKeysetWrapper;
import no.ssb.dlp.pseudo.service.sid.SidIndexUnavailableException;
import no.ssb.dlp.pseudo.service.sid.SidInfo;
import no.ssb.dlp.pseudo.service.sid.SidService;
import org.reactivestreams.Publisher;
import java.util.Optional;
/**
 * Represents a SID field to be pseudonymized.
 */
@Data
public class PseudoSIDField {
    private String name;
    private String value;
    private PseudoConfig pseudoConfig;
    private String stableIDSnapshot;

    private String sidValue;
    /**
     * Constructs a {@code PseudoSIDField} object with the specified name, value, keyset, and defaultFieldPseudoConfig.
     * If no keyset is supplied the default SID field pseudo configuration is used.
     * @param name                      The name of the SID field.
     * @param value                     The value of the SID field.
     * @param keyset                    The encrypted keyset to be used for pseudonymization.
     * @param defaultFieldPseudoConfig  The default SID field pseudo configuration.
     */
    public PseudoSIDField(String name, String value, EncryptedKeysetWrapper keyset, DefaultFieldPseudoConfig defaultFieldPseudoConfig) {
        this.name = name;
        this.value = value;

        if (keyset == null) {
            this.pseudoConfig = defaultFieldPseudoConfig.getDefualtSIDPseudoConfig();

        } else {
            this.pseudoConfig = new PseudoConfig();
            this.pseudoConfig.getRules().add(new PseudoFuncRule(PseudoSIDField.class.getName(), "**",
                    String.format("ff31(keyId=%s, strategy=SKIP)",
                            keyset.getKeysetInfo().getPrimaryKeyId())));
            this.pseudoConfig.getKeysets().add(keyset);
        }
    }

    /**
     * Maps the field value to SID using the provided {@link SidService}.
     *
     * @param sidService The {@link SidService} used to lookup and map the value to a SID.
     */
    public void mapValueToSid(SidService sidService) {

        Publisher<SidInfo> sidInfoPublisher = sidService.lookupFnr(value, Optional.ofNullable(stableIDSnapshot));

        SidInfo sidInfo = Single.fromPublisher(sidInfoPublisher)
                .doOnError(throwable -> {
                    // Handle the error and raise an appropriate response
                    throw new SidIndexUnavailableException("No SID-mapping found for fnr starting with " + Strings.padEnd(value, 6, ' ').substring(0, 6));
                })
                .blockingGet();
        this.sidValue = sidInfo.getCurrentSnr();
        //TODO: Set snapshot version
        //this.stableIDSnapshot = sidInfo.getCurrentStabelIDSnapshot();
    }

    /**
     * Creates a {@link ResponsePseudoSIDField} object with the provided encrypted value.
     *
     * @param encryptedValue The encrypted value of the SID field.
     * @return A {@link ResponsePseudoSIDField} object containing the encrypted value, field name, pseudo rules, and stable ID snapshot.
     */
    public ResponsePseudoSIDField getResponseField(String encryptedValue) {
        return new ResponsePseudoSIDField(encryptedValue, name, pseudoConfig, stableIDSnapshot);
    }

}

@Data
@AllArgsConstructor
class ResponsePseudoSIDField {
    private String value;
    private String fieldName;
    private PseudoConfig pseudoRules;
    private String stableIDSnapshot;

}