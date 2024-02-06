package no.ssb.dlp.pseudo.service.pseudo.metadata;

import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.ReplayProcessor;
import lombok.Value;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Value
public class PseudoMetadataProcessor {

    String correlationId;
    Map<String, Set<FieldMetadata>> uniqueMetadataPaths = new LinkedHashMap<>();
    ReplayProcessor<FieldMetadata> publishProcessor = ReplayProcessor.create();

    public PseudoMetadataProcessor(String correlationId) {
        this.correlationId = correlationId;
    }
    public void add(final FieldMetadata metadata) {
        Set<FieldMetadata> rules = uniqueMetadataPaths.computeIfAbsent(metadata.getDataElementPath(), k -> new HashSet<>());
        if (rules.add(metadata)) {
            publishProcessor.onNext(metadata);
        }
    }
    public FlowableProcessor<FieldMetadata> toFlowableProcessor() {
        return publishProcessor;
    }
}
