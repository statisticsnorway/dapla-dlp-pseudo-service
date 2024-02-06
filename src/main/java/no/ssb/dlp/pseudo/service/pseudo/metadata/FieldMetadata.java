package no.ssb.dlp.pseudo.service.pseudo.metadata;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class FieldMetadata {

    // Type of stable ID identifier that is used prior to pseudonymization. Currently only FREG_SNR is supported.
    public static final String STABLE_IDENTIFIER_TYPE = "FREG_SNR";

    String shortName;
    String dataElementPath;
    String dataElementPattern;
    String encryptionKeyReference;
    String encryptionAlgorithm;
    String stableIdentifierVersion;
    String stableIdentifierType;
    Map<String, String> encryptionAlgorithmParameters;
}