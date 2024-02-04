package no.ssb.dlp.pseudo.service.pseudo.metadata;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import no.ssb.dlp.pseudo.core.field.FieldDescriptor;
import no.ssb.dlp.pseudo.core.func.PseudoFuncRule;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class PseudoMetadataEvent {
    @NonNull
    private final FieldDescriptor field;
    @NonNull
    private final PseudoFuncRule rule;
    private final String algorithm;
    Map<String, String> config;
    Map<String, String> metadata;
    List<String> warnings;
}
