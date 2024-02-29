package no.ssb.dlp.pseudo.service.accessgroups;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
@Introspected
@Serdeable.Deserializable
public record LookupResponse(/* The resource name of the looked-up Group. */ String name) {
    public String getGroupName() {
        return name != null ? name.substring(name.lastIndexOf('/') + 1) : null;
    }
}