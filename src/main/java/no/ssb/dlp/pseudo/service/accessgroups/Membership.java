package no.ssb.dlp.pseudo.service.accessgroups;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

/**
 * A membership within the Cloud Identity Groups API.
 * A Membership defines a relationship between a Group and an entity belonging to that Group, referred to as a "member".
 */
@Builder
@Jacksonized
@Introspected
@Serdeable.Deserializable
public record Membership (String name, EntityKey preferredMemberKey) {}
