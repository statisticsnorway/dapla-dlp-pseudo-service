package no.ssb.dlp.pseudo.service.sid;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
@Serdeable
@Introspected
public record SidRequest(String fnr, String snr, String datasetExtractionSnapshotTime) {}