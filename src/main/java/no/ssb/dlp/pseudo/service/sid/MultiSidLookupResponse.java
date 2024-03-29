package no.ssb.dlp.pseudo.service.sid;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Builder
@Jacksonized
@Introspected
@Serdeable
public record MultiSidLookupResponse (List<String> missing, String datasetExtractionSnapshotTime) {}