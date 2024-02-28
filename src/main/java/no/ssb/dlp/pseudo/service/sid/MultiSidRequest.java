package no.ssb.dlp.pseudo.service.sid;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Data
@Builder
@Jacksonized
public class MultiSidRequest {
    private final List<String> fnrList;
    private final List<String> snrList;
    private final String datasetExtractionSnapshotTime;
}
