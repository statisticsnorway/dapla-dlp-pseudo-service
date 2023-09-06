package no.ssb.dlp.pseudo.service.sid;

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.async.publisher.Publishers;
import lombok.RequiredArgsConstructor;
import no.ssb.dlp.pseudo.service.sid.local.SidCache;
import org.reactivestreams.Publisher;

import javax.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This class implements a local SID service, and should only be used for local end-to-end testing.
 */
@Singleton
@RequiredArgsConstructor
@Requires(env = "local-sid")
class LocalSidService implements SidService {

    private final SidCache sidCache;

    @Override
    public Publisher<SidInfo> lookupFnr(String fnr, Optional<String> snapshot) {
        String currentSnr = sidCache.getCurrentSnrForFnr(fnr)
                .orElseThrow(() -> new LocalSidService.NoSidMappingFoundException("No SID matching fnr=" + fnr));
        return Publishers.just(sidCache.getCurrentFnrForSnr(currentSnr).map(currentFnr ->
                        new SidInfo.SidInfoBuilder().snr(currentSnr).fnr(currentFnr).build())
                .orElse(null));
    }

    @Override
    public Publisher<SidInfo> lookupSnr(String snr, Optional<String> snapshot) {
        String currentFnr = sidCache.getCurrentFnrForSnr(snr)
                .orElseThrow(() -> new LocalSidService.NoSidMappingFoundException("No SID matching snr=" + snr));
        return Publishers.just(sidCache.getCurrentSnrForFnr(currentFnr).map(currentSnr ->
                        new SidInfo.SidInfoBuilder().snr(currentSnr).fnr(currentFnr).build())
                .orElse(null));
    }

    @Override
    public Publisher<Map<String, SidInfo>> lookupFnr(List<String> fnrList, Optional<String> snapshot) {
        return Publishers.just(fnrList.stream().map(fnr -> {
                    String currentSnr = sidCache.getCurrentSnrForFnr(fnr).orElseThrow(() ->
                            new LocalSidService.NoSidMappingFoundException("No SID matching fnr=" + fnr));
                    return sidCache.getCurrentFnrForSnr(currentSnr).map(currentFnr ->
                                    new SidInfo.SidInfoBuilder().snr(currentSnr).fnr(currentFnr).build())
                            .orElse(null);
                }).collect(Collectors.toMap(SidInfo::getFnr, sidInfo -> sidInfo))
        );
    }

    @Override
    public Publisher<VersionInfo> getSnapshots() {
        return Publishers.just(VersionInfo.builder().items(List.of("2023_04_25-12_35_40_6495")).build());
    }

    public static class NoSidMappingFoundException extends RuntimeException {
        public NoSidMappingFoundException(String message) {
            super(message);
        }
    }

}
