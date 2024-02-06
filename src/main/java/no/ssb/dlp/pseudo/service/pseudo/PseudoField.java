package no.ssb.dlp.pseudo.service.pseudo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.processors.FlowableProcessor;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import no.ssb.dlp.pseudo.core.PseudoOperation;
import no.ssb.dlp.pseudo.core.func.PseudoFuncRule;
import no.ssb.dlp.pseudo.core.map.RecordMapProcessor;
import no.ssb.dlp.pseudo.core.tink.model.EncryptedKeysetWrapper;
import no.ssb.dlp.pseudo.core.util.Json;
import no.ssb.dlp.pseudo.service.pseudo.metadata.FieldMetadata;

import java.util.*;

/**
 * Represents a field to be pseudonymized.
 */
@Data
@Slf4j
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
    public Flowable<String> process(PseudoConfigSplitter pseudoConfigSplitter,
                                          RecordMapProcessorFactory recordProcessorFactory,
                                          List<String> values,
                                          PseudoOperation pseudoOperation,
                                          String correlationId) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        List<PseudoConfig> pseudoConfigs = pseudoConfigSplitter.splitIfNecessary(this.getPseudoConfig());

        RecordMapProcessor<FieldMetadata> recordMapProcessor;
        switch (pseudoOperation){
            case PSEUDONYMIZE -> recordMapProcessor = recordProcessorFactory.
                    newPseudonymizeRecordProcessor(pseudoConfigs, correlationId);
            case DEPSEUDONYMIZE -> recordMapProcessor = recordProcessorFactory.
                    newDepseudonymizeRecordProcessor(pseudoConfigs, correlationId);
            default -> throw new RuntimeException(
                    String.format("Pseudo operation \"%s\" not supported for this method", pseudoOperation));
        }
        Completable preprocessor = getPreprocessor(values, recordMapProcessor);
        final FlowableProcessor<FieldMetadata> metadataProcessor = recordMapProcessor.getMetadataProcessor().toFlowableProcessor();
        // Metadata will be processes in parallel with the data, but must be collected separately
        final Flowable<String> metadata = Flowable.fromPublisher(metadataProcessor).map(Json::from);

        Flowable<String> result = preprocessor.andThen(Flowable.fromIterable(values))
                .map(value -> recordMapProcessor.process(Map.of(this.getName(), value)).get(this.getName()))
                .map(Json::from)
                .doOnError(throwable -> {
                    log.error("Response failed", throwable);
                    metadataProcessor.onError(throwable);
                })
                .doOnComplete(() -> {
                    log.info("{} took {}", PseudoOperation.REPSEUDONYMIZE, stopwatch.stop().elapsed());
                    // Signal the metadataProcessor to stop collecting metadata
                    metadataProcessor.onComplete();
                });

        return PseudoResponseSerializer.serialize(result, metadata);
    }

    /**
     * Creates a Flowable that processes each value of the field, by applying the configured pseudo rules using a recordMapProcessor.
     * This variant of the process() method is intended for the "repseudonymize" operation.
     * @param recordProcessorFactory The RecordMapProcessorFactory instance to use for creating a new PseudonymizeRecordProcessor.
     * @param values                 The values to be processed.
     * @return A Flowable stream that processes the field values by applying the configured pseudo rules, and returns them as a lists of strings.
     */
    public Flowable<String> process(RecordMapProcessorFactory recordProcessorFactory,
                                          List<String> values,
                                          PseudoField targetPseudoField,
                                          String correlationId) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        PseudoConfig targetPseudoConfig = targetPseudoField.getPseudoConfig();
        RecordMapProcessor<FieldMetadata> recordMapProcessor = recordProcessorFactory.
                newRepseudonymizeRecordProcessor(this.getPseudoConfig(), targetPseudoConfig, correlationId);
        Completable preprocessor = getPreprocessor(values, recordMapProcessor);
        final FlowableProcessor<FieldMetadata> metadataProcessor = recordMapProcessor.getMetadataProcessor().toFlowableProcessor();
        // Metadata will be processes in parallel with the data, but must be collected separately
        final Flowable<String> metadata = Flowable.fromPublisher(metadataProcessor).map(Json::from);

        Flowable<String> result = preprocessor.andThen(Flowable.fromIterable(values))
                .map(value -> recordMapProcessor.process(Map.of(this.getName(), value)).get(this.getName()))
                .map(Json::from)
                .doOnError(throwable -> {
                    log.error("Response failed", throwable);
                    metadataProcessor.onError(throwable);
                })
                .doOnComplete(() -> {
                    log.info("{} took {}", PseudoOperation.REPSEUDONYMIZE, stopwatch.stop().elapsed());
                    // Signal the metadataProcessor to stop collecting metadata
                    metadataProcessor.onComplete();
                });
        return PseudoResponseSerializer.serialize(result, metadata);
    }

    protected Completable getPreprocessor(List<String> values, RecordMapProcessor<FieldMetadata> recordMapProcessor) {
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
}