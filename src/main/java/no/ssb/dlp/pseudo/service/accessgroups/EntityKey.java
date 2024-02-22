package no.ssb.dlp.pseudo.service.accessgroups;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

/**
 * A unique identifier for an entity in the Cloud Identity Groups API.
 * An entity can represent either a group with an optional namespace or a user without a namespace. The combination of
 * id and namespace must be unique; however, the same id can be used with different namespaces.
 */
@Builder
@Jacksonized
@Introspected
@Serdeable.Deserializable
public record EntityKey(String id, String namespace) {}