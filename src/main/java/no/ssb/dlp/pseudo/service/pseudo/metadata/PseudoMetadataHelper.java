package no.ssb.dlp.pseudo.service.pseudo.metadata;

import lombok.Value;
import no.ssb.dapla.dlp.pseudo.func.map.MapFuncConfig;
import no.ssb.dapla.metadata.EncryptionAlgorithmParameter;
import no.ssb.dapla.metadata.PseudoVariable;
import no.ssb.dlp.pseudo.core.func.PseudoFuncDeclaration;

import java.util.Optional;
import java.util.Set;

import static no.ssb.dlp.pseudo.core.func.PseudoFuncNames.*;

@Value
public class PseudoMetadataHelper {

    // Type of stable ID identifier that is used prior to pseudonymization. Currently only FREG_SNR is supported.
    public static final String STABLE_IDENTIFIER_TYPE = "FREG_SNR";

    //TODO: Make this into using flowables and groupBy / flatMap
    public static PseudoVariable toPseudoVariable(Set<FieldMetadata> entry) {
        final Optional<FieldMetadata> sid_rule = entry.stream()
                .filter(r -> r.getFunc().startsWith(MAP_SID)).findFirst();
        final FieldMetadata other_rule = entry.stream()
                .filter(r -> !r.getFunc().startsWith(MAP_SID)).findFirst().orElseThrow();
        return PseudoVariable.builder()
                .withShortName(other_rule.getName())
                .withSourceVariableDatatype(PseudoVariable.SourceVariableDataType.STRING)
                .withDataElementPath(other_rule.getPath())
                .withDataElementPattern(other_rule.getPattern())
                .withEncryptionAlgorithm(other_rule.getAlgorithm())
                .withEncryptionAlgorithmParameters(PseudoFuncDeclaration.fromString(other_rule.getFunc())
                        .getArgs().entrySet().stream()
                        .map(e -> EncryptionAlgorithmParameter.builder()
                                .withAdditionalProperty(e.getKey(), e.getValue())
                                .build())
                        .toList())
                .withStableIdentifierType(sid_rule.map(fieldPseudoFuncRule -> STABLE_IDENTIFIER_TYPE).orElse(null))
                .withStableIdentifierVersion(sid_rule.map(fieldPseudoFuncRule ->
                                fieldPseudoFuncRule.getMetadata().get(MapFuncConfig.Param.SNAPSHOT_DATE))
                        .orElse(null))
                .build();
    }


}
