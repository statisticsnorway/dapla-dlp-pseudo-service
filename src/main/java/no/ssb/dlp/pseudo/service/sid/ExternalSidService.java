package no.ssb.dlp.pseudo.service.sid;

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.async.publisher.Publishers;
import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;

import jakarta.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Singleton
@RequiredArgsConstructor
@Requires(notEnv = "local-sid")
public class ExternalSidService implements SidService {

    private final SidClient sidClient;

    @Override
    public Publisher<SidInfo> lookupFnr(String fnr, Optional<String> snapshot) {
        return sidClient.lookup(new SidRequest.SidRequestBuilder().fnr(fnr)
                .datasetExtractionSnapshotTime(snapshot.orElse(null)).build());
    }

    @Override
    public Publisher<SidInfo> lookupSnr(String snr, Optional<String> snapshot) {
        return sidClient.lookup(new SidRequest.SidRequestBuilder().snr(snr)
                .datasetExtractionSnapshotTime(snapshot.orElse(null)).build());
    }

    @Override
    public Publisher<Map<String, SidInfo>> lookupFnr(List<String> fnrList, Optional<String> snapshot) {
        return Publishers.map(sidClient.lookup(
                        new MultiSidRequest.MultiSidRequestBuilder().fnrList(fnrList)
                        .datasetExtractionSnapshotTime(snapshot.orElse(null)).build()
                ), MultiSidResponse::toMap
        );
    }

    public Publisher<Map<String, SidInfo>> lookupSnr(List<String> snrList, Optional<String> snapshot) {
        return Publishers.map(sidClient.lookup(
                        new MultiSidRequest.MultiSidRequestBuilder().snrList(snrList)
                                .datasetExtractionSnapshotTime(snapshot.orElse(null)).build()
                ), MultiSidResponse::toMap
        );
    }

    @Override
    public Publisher<MultiSidLookupResponse> lookupMissing(List<String> fnrList, Optional<String> snapshot) {
        return Publishers.map(sidClient.lookup(
                        new MultiSidRequest.MultiSidRequestBuilder().fnrList(fnrList)
                                .datasetExtractionSnapshotTime(snapshot.orElse(null)).build()
                ), response ->
                    MultiSidLookupResponse.builder()
                            .missing(response.getMissing())
                            .datasetExtractionSnapshotTime(response.getDatasetExtractionSnapshotTime())
                        .build()
        );
    }

    @Override
    public Publisher<SnapshotInfo> getSnapshots() {
        return sidClient.snapshots();
    }
}
