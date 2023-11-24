package no.ssb.dlp.pseudo.service.accessgroups;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class LookupResponse {
    // The resource name of the looked-up Group.
    private final String name;

    public String getGroupName() {
        return name != null ? name.substring(name.lastIndexOf('/') + 1) : null;
    }
}
