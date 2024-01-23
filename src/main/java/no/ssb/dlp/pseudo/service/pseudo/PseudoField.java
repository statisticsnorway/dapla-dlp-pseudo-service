package no.ssb.dlp.pseudo.service.pseudo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import lombok.*;
import no.ssb.dlp.pseudo.core.PseudoOperation;
import no.ssb.dlp.pseudo.core.func.PseudoFuncRule;
import no.ssb.dlp.pseudo.core.map.RecordMapProcessor;
import no.ssb.dlp.pseudo.core.tink.model.EncryptedKeysetWrapper;

import java.util.*;

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
    protected PseudoConfig pseudoConfig;

    /**
     * Constructs a {@code PseudoField} object with the specified name, keyset, pseudoConfig. If no keyset is supplied
     * a default pseudo configuration is used.
     *
     * @param name       The name of the field.
     * @param pseudoFunc The pseudo function definition.
     * @param keyset     The encrypted keyset to be used for pseudonymization.
     */
    public PseudoField(String name, String pseudoFunc, EncryptedKeysetWrapper keyset) {
        this.name = name;

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
     * This variant of the process() method is intended for "pseudonymize" and "depseudonymize" operations.
     * @param pseudoConfigSplitter   The PseudoConfigSplitter instance to use for splitting pseudo configurations.
     * @param recordProcessorFactory The RecordMapProcessorFactory instance to use for creating a new PseudonymizeRecordProcessor.
     * @param values                 The values to be processed.
     * @return A Flowable stream that processes the field values by applying the configured pseudo rules, and returns them as a lists of strings.
     */
    public Flowable<List<Object>> process(PseudoConfigSplitter pseudoConfigSplitter,
                                          RecordMapProcessorFactory recordProcessorFactory,
                                          List<String> values,
                                          PseudoOperation pseudoOperation) {
        List<PseudoConfig> pseudoConfigs = pseudoConfigSplitter.splitIfNecessary(this.getPseudoConfig());

        RecordMapProcessor recordMapProcessor;
        switch (pseudoOperation){
            case PSEUDONYMIZE -> recordMapProcessor = recordProcessorFactory.
                    newPseudonymizeRecordProcessor(pseudoConfigs);
            case DEPSEUDONYMIZE -> recordMapProcessor = recordProcessorFactory.
                    newDepseudonymizeRecordProcessor(pseudoConfigs);
            default -> throw new RuntimeException(
                    String.format("Pseudo operation \"%s\" not supported for this method", pseudoOperation));
        }
        Completable preprocessor = getPreprocessor(values, recordMapProcessor);

        return preprocessor.andThen(Flowable.fromIterable(() ->
                values.stream().map(value -> {
                    if (value == null) {
                        return Optional.empty();
                    }
                    return recordMapProcessor.process(Map.of(this.getName(), value)).get(this.getName()).toString();
                }).iterator()).buffer(BUFFER_SIZE));
    }

    /**
     * Creates a Flowable that processes each value of the field, by applying the configured pseudo rules using a recordMapProcessor.
     * This variant of the process() method is intended for the "repseudonymize" operation.
     * @param recordProcessorFactory The RecordMapProcessorFactory instance to use for creating a new PseudonymizeRecordProcessor.
     * @param values                 The values to be processed.
     * @return A Flowable stream that processes the field values by applying the configured pseudo rules, and returns them as a lists of strings.
     */
    public Flowable<List<Object>> process(RecordMapProcessorFactory recordProcessorFactory,
                                          List<String> values,
                                          PseudoField targetPseudoField) {
        PseudoConfig targetPseudoConfig = targetPseudoField.getPseudoConfig();
        RecordMapProcessor recordMapProcessor = recordProcessorFactory.
                newRepseudonymizeRecordProcessor(this.getPseudoConfig(), targetPseudoConfig);
        Completable preprocessor = getPreprocessor(values, recordMapProcessor);

        return preprocessor.andThen(Flowable.fromIterable(() ->
                values.stream().map(value -> {
                    if (value == null) {
                        return Optional.empty();
                    }
                    return recordMapProcessor.process(Map.of(this.getName(), value)).get(this.getName()).toString();
                }).iterator()).buffer(BUFFER_SIZE));
    }

    protected Completable getPreprocessor(List<String> values, RecordMapProcessor recordMapProcessor) {
        if (recordMapProcessor.hasPreprocessors()) {
            return Completable.fromPublisher(Flowable.fromIterable(() ->
                    values.stream().map(value -> {
                        if (value == null) {
                            return Optional.ofNullable(null);
                        }
                        return recordMapProcessor.init(Map.of(this.getName(), value));
                    }).iterator()));
        } else {
            return Completable.complete();
        }
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

    /**
     * Converts the {@link PseudoFieldMetadata} object to a JSON string.
     *
     * @return A {@link String} representing the JSON representation of the PseudoFieldMetadata
     * @throws JsonProcessingException if an error occurs during JSON processing
     */
    public String toJsonString() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(this);
    }
}