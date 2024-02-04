package no.ssb.dlp.pseudo.service.pseudo.metadata;

import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.ReplayProcessor;
import lombok.Value;
import no.ssb.dlp.pseudo.core.util.Json;

import java.util.HashSet;
import java.util.Set;

@Value
public class PseudoMetadataEventListener {

    PseudoMetadata metadata;
    ReplayProcessor<String> publishProcessor = ReplayProcessor.create();

    public PseudoMetadataEventListener(String correlationId) {
        metadata = new PseudoMetadata(correlationId);
    }
    public void onPseudoMetadataEvent(PseudoMetadataEvent e) {
        Set<PseudoMetadata.FieldPseudoMetadata> rules = metadata.getFields().computeIfAbsent(e.getField().getPath(), k -> new HashSet<>());
        final PseudoMetadata.FieldPseudoMetadata metadata = PseudoMetadata.FieldPseudoMetadata.builder()
                .path(e.getField().getPath())
                .name(e.getField().getName())
                .pattern(e.getRule().getPattern())
                .func(e.getRule().getFunc())
                .algorithm(e.getAlgorithm())
                .metadata(e.getMetadata())
                .build();
        if (rules.add(metadata)) {
            publishProcessor.onNext(Json.prettyFrom(metadata));
        }
    }
    public FlowableProcessor<String> getMetadataProcessor() {
        return publishProcessor;
    }
}
