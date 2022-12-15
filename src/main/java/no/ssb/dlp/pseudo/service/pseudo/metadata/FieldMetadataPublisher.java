package no.ssb.dlp.pseudo.service.pseudo.metadata;

import io.micronaut.context.event.ApplicationEventPublisher;
import lombok.RequiredArgsConstructor;
import no.ssb.dlp.pseudo.core.field.FieldDescriptor;
import no.ssb.dlp.pseudo.core.field.ValueInterceptor;

@RequiredArgsConstructor
public class FieldMetadataPublisher implements ValueInterceptor {
    private final String correlationId;

    private final ApplicationEventPublisher<PseudoMetadataEvent> eventPublisher;

    @Override
    public String apply(FieldDescriptor fieldDescriptor, String s) {
        eventPublisher.publishEvent(PseudoMetadataEvent.builder()
                        .field(fieldDescriptor)
                        .correlationId(correlationId)
                .build());
        return s;
    }
}
