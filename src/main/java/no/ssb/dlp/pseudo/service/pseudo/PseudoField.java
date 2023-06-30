package no.ssb.dlp.pseudo.service.pseudo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.micronaut.http.MutableHttpHeaders;
import io.reactivex.Flowable;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import no.ssb.dlp.pseudo.core.func.PseudoFuncRule;
import no.ssb.dlp.pseudo.core.map.RecordMapProcessor;
import no.ssb.dlp.pseudo.core.tink.model.EncryptedKeysetWrapper;

import java.util.List;
import java.util.Map;

/**
 * Represents a field to be pseudonymized.
 */
@Data
public class PseudoField {
    @Getter(AccessLevel.PROTECTED)
    private static final int BUFFER_SIZE = 10000;
    @Getter(AccessLevel.PROTECTED)
    private static final String DEFAULT_PSEUDO_FUNC = "daead(keyId=ssb-common-key-1)";

    protected String name;
    protected List<String> values;
    protected PseudoConfig pseudoConfig;

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
     * Creates a Flowable that processes each value of the field, by applying the configured pseudo rules using a recordMapProcessor.
     *
     * @param pseudoConfigSplitter   The PseudoConfigSplitter instance to use for splitting pseudo configurations.
     * @param recordProcessorFactory The RecordMapProcessorFactory instance to use for creating a new PseudonymizeRecordProcessor.
     * @return A Flowable stream that processes the field values by applying the configured pseudo rules, and returns them as a lists of strings.
     */
    public Flowable<List<String>> process(PseudoConfigSplitter pseudoConfigSplitter, RecordMapProcessorFactory recordProcessorFactory) {
        List<PseudoConfig> pseudoConfigs = pseudoConfigSplitter.splitIfNecessary(this.getPseudoConfig());

        RecordMapProcessor recordMapProcessor = recordProcessorFactory.newPseudonymizeRecordProcessor(pseudoConfigs);

        return Flowable.fromIterable(() -> this.getValues().stream()
                .map(value -> recordMapProcessor.process(Map.of(this.getName(), value))
                        .get(this.getName()).toString())
                .iterator()).buffer(BUFFER_SIZE);
    }


    /**
     * Creates a {@link PseudoFieldMetadata} object with the metadata about the preformed pseudo operations.
     *
     * @return A {@link PseudoFieldMetadata} object containing the field name and pseudo rules used.
     */
    public PseudoFieldMetadata getPseudoFieldMetadata() {
        return PseudoFieldMetadata.builder()
                .fieldName(name)
                .pseudoRules(pseudoConfig).build();
    }
}

@Builder
@Data
class PseudoFieldMetadata {
    private String fieldName;
    private PseudoConfig pseudoRules;

    public String toJsonString() throws JsonProcessingException {
        /**
         * Converts the {@link PseudoFieldMetadata} object to a JSON string.
         *
         * @return A {@link String} representing the JSON representation of the PseudoFieldMetadata
         * @throws JsonProcessingException if an error occurs during JSON processing
         */
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(this);
    }
}