package no.ssb.dlp.pseudo.service.accessgroups;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

/**
 * A membership within the Cloud Identity Groups API.
 * A Membership defines a relationship between a Group and an entity belonging to that Group, referred to as a "member".
 */
@Data
@Builder
@Jacksonized
public class Membership {
    private final String name;
    private final EntityKey preferredMemberKey;
}
