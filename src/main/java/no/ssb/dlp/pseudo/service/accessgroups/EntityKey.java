package no.ssb.dlp.pseudo.service.accessgroups;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

/**
 * A unique identifier for an entity in the Cloud Identity Groups API.
 * An entity can represent either a group with an optional namespace or a user without a namespace. The combination of
 * id and namespace must be unique; however, the same id can be used with different namespaces.
 */
@Data
@Builder
@Jacksonized
public class EntityKey {
    private final String id;
    private final String namespace;
}
