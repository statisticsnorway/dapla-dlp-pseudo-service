package no.ssb.dlp.pseudo.service.pseudo.metadata;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import no.ssb.dlp.pseudo.core.field.FieldDescriptor;

@Data
@Builder
public class PseudoMetadataEvent {
    @NonNull
    private final String correlationId;
    @NonNull
    private final FieldDescriptor field;
//    @NonNull
//    private Class datatype;
}
