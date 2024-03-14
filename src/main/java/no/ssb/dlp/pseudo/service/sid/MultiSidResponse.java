package no.ssb.dlp.pseudo.service.sid;

import io.micronaut.core.annotation.Introspected;
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
@Introspected
@Serdeable
public class MultiSidResponse {

    private final Mapping mapping;
    private final List<String> missing;
    private final String datasetExtractionSnapshotTime;

    @Builder
    @Jacksonized
    @Introspected
    @Serdeable
    public record Mapping (List<String> fnrList, List<String> snrList, List<String> snr, List<String> fnr) { }

    public Map<String, SidInfo> toMap() {
        Map<String, SidInfo> result = new HashMap<>();

        ListIterator<String> identifierIterator = getMapping().fnrList != null ?
                getMapping().fnrList().listIterator() : getMapping().snrList().listIterator();

        for (ListIterator<String> it = identifierIterator; it.hasNext(); ) {
            int index = it.nextIndex();
            result.put(it.next(), new SidInfo.SidInfoBuilder()
                    .snr(getMapping().snr().get(index))
                    .fnr(getMapping().fnr().get(index))
                    .datasetExtractionSnapshotTime(getDatasetExtractionSnapshotTime())
                    .build());
        }
        return result;
    }

}
