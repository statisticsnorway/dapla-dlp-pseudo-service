package no.ssb.dlp.pseudo.service.accessgroups;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Data
@Builder
@Jacksonized
@Introspected
@Serdeable.Deserializable
public class MembershipResponse {
    private final List<Membership> memberships;
    private final String nextPageToken;
}