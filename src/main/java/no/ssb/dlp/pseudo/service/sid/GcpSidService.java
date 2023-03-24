package no.ssb.dlp.pseudo.service.sid;

import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;

@RequiredArgsConstructor
@Singleton
@Requires(property = SidServiceConfig.PREFIX + ".impl", value = "GCP")
class GcpSidService implements SidService {

    @NonNull
    private final SidClient sidClient;

    public Publisher<SidInfo> lookupFnr(String fnr) {
        return sidClient.lookup(new SidRequest.SidRequestBuilder().fnr(fnr).build());
    }

    public Publisher<SidInfo> lookupSnr(String snr) {
        return sidClient.lookup(new SidRequest.SidRequestBuilder().fnr(snr).build());
    }

}
