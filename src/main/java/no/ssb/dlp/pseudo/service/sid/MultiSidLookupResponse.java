package no.ssb.dlp.pseudo.service.sid;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Data
@Builder
@Jacksonized
public class MultiSidLookupResponse {

    private final List<String> missing;
    private final String datasetExtractionSnapshotTime;
}
