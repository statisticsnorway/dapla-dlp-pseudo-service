package no.ssb.dlp.pseudo.service.sid;

import com.google.common.base.Strings;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.async.publisher.Publishers;
import lombok.RequiredArgsConstructor;
import no.ssb.dlp.pseudo.service.sid.local.SidCache;
import org.reactivestreams.Publisher;

import jakarta.inject.Singleton;
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
                .orElseThrow(() -> new LocalSidService.NoSidMappingFoundException("No SID matching fnr starting from="
                        + Strings.padEnd(fnr, 6, ' ').substring(0, 6)));
        return Publishers.just(sidCache.getCurrentFnrForSnr(currentSnr).map(currentFnr ->
                        new SidInfo.SidInfoBuilder().snr(currentSnr).fnr(currentFnr).build())
                .orElse(null));
    }

    @Override
    public Publisher<SidInfo> lookupSnr(String snr, Optional<String> snapshot) {
        String currentFnr = sidCache.getCurrentFnrForSnr(snr)
                .orElseThrow(() -> new LocalSidService.NoSidMappingFoundException("No SID matching snr starting from="
                        + Strings.padEnd(snr, 4, ' ').substring(0, 4)));
        return Publishers.just(sidCache.getCurrentSnrForFnr(currentFnr).map(currentSnr ->
                        new SidInfo.SidInfoBuilder().snr(currentSnr).fnr(currentFnr).build())
                .orElse(null));
    }

    @Override
    public Publisher<Map<String, SidInfo>> lookupFnr(List<String> fnrList, Optional<String> snapshot) {
        return Publishers.just(fnrList.stream().map(fnr -> {
                    String currentSnr = sidCache.getCurrentSnrForFnr(fnr).orElseThrow(() ->
                            new LocalSidService.NoSidMappingFoundException("No SID matching fnr starting from="
                                    + Strings.padEnd(fnr, 6, ' ').substring(0, 6)));
                    return sidCache.getCurrentFnrForSnr(currentSnr).map(currentFnr ->
                                    new SidInfo.SidInfoBuilder().snr(currentSnr).fnr(currentFnr)
                                            .datasetExtractionSnapshotTime(snapshot.orElse(null)).build())
                            .orElse(null);
                }).collect(Collectors.toMap(SidInfo::fnr, sidInfo -> sidInfo))
        );
    }

    @Override
    public Publisher<Map<String, SidInfo>> lookupSnr(List<String> snrList, Optional<String> snapshot) {
        return Publishers.just(snrList.stream().map(snr -> {
                    String currentFnr = sidCache.getCurrentFnrForSnr(snr).orElseThrow(() ->
                            new LocalSidService.NoSidMappingFoundException("No SID matching snr starting from="
                                    + Strings.padEnd(snr, 4, ' ').substring(0, 4)));
                    return sidCache.getCurrentSnrForFnr(currentFnr).map(currentSnr ->
                                    new SidInfo.SidInfoBuilder().fnr(currentFnr).snr(currentSnr)
                                            .datasetExtractionSnapshotTime(snapshot.orElse(null)).build())
                            .orElse(null);
                }).collect(Collectors.toMap(SidInfo::snr, sidInfo -> sidInfo))
        );
    }

    @Override
    public Publisher<MultiSidLookupResponse> lookupMissing(List<String> fnrList, Optional<String> snapshot) {
        return Publishers.just(MultiSidLookupResponse.builder().missing(fnrList.stream().filter(fnr ->
                sidCache.getCurrentSnrForFnr(fnr).isEmpty()).collect(Collectors.toList())).build()
        );
    }

    @Override
    public Publisher<SnapshotInfo> getSnapshots() {
        return Publishers.just(SnapshotInfo.builder().items(List.of("2023-04-25")).build());
    }

    public static class NoSidMappingFoundException extends RuntimeException {
        public NoSidMappingFoundException(String message) {
            super(message);
        }
    }

}
