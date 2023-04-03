package no.ssb.dlp.pseudo.service.sid;

import io.micronaut.context.annotation.Requires;
import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;

import javax.inject.Singleton;
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
}
