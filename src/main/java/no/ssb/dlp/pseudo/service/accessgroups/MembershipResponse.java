package no.ssb.dlp.pseudo.service.accessgroups;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Data
@Builder
@Jacksonized
public class MembershipResponse {
    private final List<Membership> memberships;
    private final String nextPageToken;
}
