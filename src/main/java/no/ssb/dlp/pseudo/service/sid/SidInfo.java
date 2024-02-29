package no.ssb.dlp.pseudo.service.sid;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
@Introspected
@Serdeable
public record SidInfo (String fnr, String snr, String datasetExtractionSnapshotTime) {}