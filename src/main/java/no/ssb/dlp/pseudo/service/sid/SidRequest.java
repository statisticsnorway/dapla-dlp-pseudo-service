package no.ssb.dlp.pseudo.service.sid;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class SidRequest {
    private final String fnr;
    private final String snr;
    private final String datasetExtractionSnapshotTime;
}
