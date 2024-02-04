package no.ssb.dlp.pseudo.service.pseudo.metadata;

import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.annotation.Async;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Singleton
public class PseudoMetadataService {

    private final Map<String, PseudoMetadata> metadataStore = Collections.synchronizedMap(new LinkedHashMap<>());

    @EventListener
    @Async
    public void onPseudoMetadataEvent(PseudoMetadataEvent e) {
        String id = "dummy";
        PseudoMetadata metadata = metadataStore.computeIfAbsent(id, m -> new PseudoMetadata(id));
        Set<PseudoMetadata.FieldPseudoMetadata> rules = metadata.getFields().computeIfAbsent(e.getField().getPath(), k -> new HashSet<>());
        rules.add(PseudoMetadata.FieldPseudoMetadata.builder()
                .path(e.getField().getPath())
                .name(e.getField().getName())
                .pattern(e.getRule().getPattern())
                .func(e.getRule().getFunc())
                .algorithm(e.getAlgorithm())
                .metadata(e.getMetadata())
                .build());

        List<String> warnings = metadata.getWarnings().computeIfAbsent(e.getField().getPath(), k -> new ArrayList<>());
        warnings.addAll(e.getWarnings());
    }

    public Optional<PseudoMetadata> findById(String id) {
        return Optional.ofNullable(metadataStore.get(id));
    }

    public Collection<PseudoMetadata> listAll() {
        return metadataStore.values();
    }

}
