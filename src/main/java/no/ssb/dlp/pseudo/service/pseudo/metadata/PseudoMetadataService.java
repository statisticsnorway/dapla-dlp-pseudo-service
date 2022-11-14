package no.ssb.dlp.pseudo.service.pseudo.metadata;

import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.annotation.Async;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Singleton
public class PseudoMetadataService {

    private final Map<String, PseudoMetadata> metadataStore = new LinkedHashMap<>();

    @EventListener
    @Async
    public void onPseudoMetadataEvent(PseudoMetadataEvent e) {
        String id = e.getCorrelationId();
        PseudoMetadata metadata = metadataStore.get(id);
        if (metadata == null) {
            metadata = new PseudoMetadata(id);
            metadataStore.put(id, metadata);
        }

        metadata.getFields().add(e.getField());
    }

    public Optional<PseudoMetadata> findById(String id) {
        return Optional.ofNullable(metadataStore.get(id));
    }

    public Collection<PseudoMetadata> listAll() {
        return metadataStore.values();
    }

}
