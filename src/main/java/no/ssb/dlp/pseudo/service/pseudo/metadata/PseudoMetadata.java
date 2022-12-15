package no.ssb.dlp.pseudo.service.pseudo.metadata;

import lombok.Value;
import no.ssb.dlp.pseudo.core.field.FieldDescriptor;
import no.ssb.dlp.pseudo.core.util.Json;

import java.util.LinkedHashSet;
import java.util.Set;

@Value
public class PseudoMetadata {

    private final String id;
    private final Set<FieldDescriptor> fields = new LinkedHashSet<>();

    public String toJson() {
        return Json.prettyFrom(this);
    }
}
