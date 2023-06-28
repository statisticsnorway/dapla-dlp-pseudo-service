package no.ssb.dlp.pseudo.service.sid;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.Set;

@Data
@Builder
@Jacksonized
public class MultiSidRequest {
    private final Set<String> fnrList;
    private final String datasetExtractionSnapshotTime;
}
