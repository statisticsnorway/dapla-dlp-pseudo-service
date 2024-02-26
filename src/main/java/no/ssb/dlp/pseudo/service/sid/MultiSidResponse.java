package no.ssb.dlp.pseudo.service.sid;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

@Data
@Builder
@Jacksonized
@Serdeable
public class MultiSidResponse {

    private final Mapping mapping;
    private final List<String> missing;
    private final String datasetExtractionSnapshotTime;

    @Data
    @Builder
    @Jacksonized
    public static class Mapping {
        private final List<String> fnrList;
        private final List<String> snr;
        private final List<String> fnr;
    }

    public Map<String, SidInfo> toMap() {
        Map<String, SidInfo> result = new HashMap<>();
        for (ListIterator<String> it = getMapping().getFnrList().listIterator(); it.hasNext(); ) {
            int index = it.nextIndex();
            result.put(it.next(), new SidInfo.SidInfoBuilder()
                    .snr(getMapping().getSnr().get(index))
                    .fnr(getMapping().getFnr().get(index))
                    .datasetExtractionSnapshotTime(getDatasetExtractionSnapshotTime())
                    .build());
        }
        return result;
    }

}
