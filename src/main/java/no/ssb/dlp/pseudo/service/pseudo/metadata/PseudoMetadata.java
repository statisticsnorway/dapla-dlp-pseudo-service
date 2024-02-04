package no.ssb.dlp.pseudo.service.pseudo.metadata;

import lombok.Builder;
import lombok.Value;
import no.ssb.dapla.dlp.pseudo.func.map.MapFuncConfig;
import no.ssb.dapla.metadata.EncryptionAlgorithmParameter;
import no.ssb.dapla.metadata.PseudoVariable;
import no.ssb.dapla.metadata.PseudonymizationMetadata;
import no.ssb.dlp.pseudo.core.func.PseudoFuncDeclaration;
import no.ssb.dlp.pseudo.core.util.Json;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static no.ssb.dlp.pseudo.core.func.PseudoFuncNames.*;

@Value
public class PseudoMetadata {

    // Type of stable ID identifier that is used prior to pseudonymization. Currently only FREG_SNR is supported.
    public static final String STABLE_IDENTIFIER_TYPE = "FREG_SNR";

    private final String id;
    private final Map<String, Set<FieldPseudoMetadata>> fields = new LinkedHashMap<>();
    private final Map<String, List<String>> warnings = new LinkedHashMap<>();

    public String toJson() {
        return Json.prettyFrom(this);
    }

    public PseudonymizationMetadata toPseudonymizationMetadata() {
        PseudonymizationMetadata metadata = PseudonymizationMetadata.builder()
            .withPseudoVariables(fields.entrySet().stream().map(entry -> {

                final Optional<FieldPseudoMetadata> sid_rule = entry.getValue().stream()
                        .filter(r -> r.getFunc().startsWith(MAP_SID)).findFirst();
                final FieldPseudoMetadata other_rule = entry.getValue().stream()
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
            ).toList()).build();
        return metadata;
    }

    @Value
    @Builder
    public static class FieldPseudoMetadata {

        private final String path;
        private final String name;
        private final String pattern;
        private final String func;
        private final String algorithm;
        Map<String, String> metadata;
    }


}
