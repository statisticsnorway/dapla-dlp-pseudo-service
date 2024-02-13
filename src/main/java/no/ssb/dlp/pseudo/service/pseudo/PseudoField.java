package no.ssb.dlp.pseudo.service.pseudo;

import com.google.common.base.Stopwatch;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.ssb.dlp.pseudo.core.PseudoOperation;
import no.ssb.dlp.pseudo.core.func.PseudoFuncRule;
import no.ssb.dlp.pseudo.core.map.RecordMapProcessor;
import no.ssb.dlp.pseudo.core.tink.model.EncryptedKeysetWrapper;
import no.ssb.dlp.pseudo.core.util.Json;
import no.ssb.dlp.pseudo.service.pseudo.metadata.FieldMetric;
import no.ssb.dlp.pseudo.service.pseudo.metadata.PseudoMetadataProcessor;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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

        RecordMapProcessor<PseudoMetadataProcessor> recordMapProcessor;
        switch (pseudoOperation){
            case PSEUDONYMIZE -> recordMapProcessor = recordProcessorFactory.
                    newPseudonymizeRecordProcessor(pseudoConfigs, correlationId);
            case DEPSEUDONYMIZE -> recordMapProcessor = recordProcessorFactory.
                    newDepseudonymizeRecordProcessor(pseudoConfigs, correlationId);
            default -> throw new RuntimeException(
                    String.format("Pseudo operation \"%s\" not supported for this method", pseudoOperation));
        }
        Completable preprocessor = getPreprocessor(values, recordMapProcessor);
        // Metadata will be processes in parallel with the data, but must be collected separately
        final PseudoMetadataProcessor metadataProcessor = recordMapProcessor.getMetadataProcessor();
        final Flowable<String> metadata = Flowable.fromPublisher(metadataProcessor.getMetadata());
        final Flowable<String> logs = Flowable.fromPublisher(metadataProcessor.getLogs());
        final Flowable<String> metrics = Flowable.fromPublisher(metadataProcessor.getMetrics());

        Flowable<String> result = preprocessor.andThen(Flowable.fromIterable(values.stream()
                        .map(v -> mapOptional(v, recordMapProcessor, metadataProcessor)).toList()
                ))
                .map(v -> v.map(Json::from).orElse("null"))
                .doOnError(throwable -> {
                    log.error("Response failed", throwable);
                    recordMapProcessor.getMetadataProcessor().onErrorAll(throwable);
                })
                .doOnComplete(() -> {
                    log.info("{} took {}", pseudoOperation, stopwatch.stop().elapsed());
                    // Signal the metadataProcessor to stop collecting metadata
                    recordMapProcessor.getMetadataProcessor().onCompleteAll();
                });

        return PseudoResponseSerializer.serialize(result, metadata, logs, metrics);
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
        RecordMapProcessor<PseudoMetadataProcessor> recordMapProcessor = recordProcessorFactory.
                newRepseudonymizeRecordProcessor(this.getPseudoConfig(), targetPseudoConfig, correlationId);
        Completable preprocessor = getPreprocessor(values, recordMapProcessor);
        // Metadata will be processes in parallel with the data, but must be collected separately
        final PseudoMetadataProcessor metadataProcessor = recordMapProcessor.getMetadataProcessor();
        final Flowable<String> metadata = Flowable.fromPublisher(metadataProcessor.getMetadata());
        final Flowable<String> logs = Flowable.fromPublisher(metadataProcessor.getLogs());
        final Flowable<String> metrics = Flowable.fromPublisher(metadataProcessor.getMetrics());

        Flowable<String> result = preprocessor.andThen(Flowable.fromIterable(values.stream()
                        .map(v -> mapOptional(v, recordMapProcessor, metadataProcessor)).toList()
                ))
                .map(v -> v.map(Json::from).orElse("null"))
                .doOnError(throwable -> {
                    log.error("Response failed", throwable);
                    metadataProcessor.onErrorAll(throwable);
                })
                .doOnComplete(() -> {
                    log.info("{} took {}", PseudoOperation.REPSEUDONYMIZE, stopwatch.stop().elapsed());
                    // Signal the metadataProcessor to stop collecting metadata
                    metadataProcessor.onCompleteAll();
                });
        return PseudoResponseSerializer.serialize(result, metadata, logs, metrics);
    }

    private Optional<Object> mapOptional(String v, RecordMapProcessor<PseudoMetadataProcessor> recordMapProcessor,
                                         PseudoMetadataProcessor metadataProcessor) {
        if (v == null) {
            metadataProcessor.addMetric(FieldMetric.NULL_VALUE);
            return Optional.empty();
        } else {
            return Optional.of(recordMapProcessor.process(Map.of(this.getName(), v)).get(this.getName()));
        }
    }

    protected Completable getPreprocessor(List<String> values, RecordMapProcessor<PseudoMetadataProcessor> recordMapProcessor) {
        if (recordMapProcessor.hasPreprocessors()) {
            return Completable.fromPublisher(Flowable.fromIterable(values.stream()
                    .filter(Objects::nonNull)
                    .map(v -> recordMapProcessor.init(Map.of(this.getName(), v)))
                    .toList())
            );
        } else {
            return Completable.complete();
        }
    }
}