package no.ssb.dlp.pseudo.service.sid;

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.async.publisher.Publishers;
import no.ssb.dlp.pseudo.service.sid.local.SidReader;
import org.reactivestreams.Publisher;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * This class implements a local SID service, and should only be used for local end-to-end testing.
 */
@Singleton
@Requires(env = "local-sid")
class LocalSidService implements SidService {

    private final Map<String, String> fnrToCurrentSnr = new HashMap<>();
    private final Map<String, String> snrToCurrentFnr = new HashMap<>();

    public LocalSidService() {
        String filePath = "src/test/resources/freg/snr-kat-sample";
        SidReader sidReader = new SidReader();
        sidReader.readSidsFromFile(filePath).subscribe(
                // onNext
                sidItem -> {
                    fnrToCurrentSnr.put(sidItem.getFnr(), sidItem.getCurrentSnr());
                    snrToCurrentFnr.put(sidItem.getSnr(), sidItem.getCurrentFnr());
                }
        );
    }

    @Override
    public Publisher<SidInfo> lookupFnr(String fnr, Optional<String> snapshot) {
        String currentSnr = getCurrentSnrForFnr(fnr)
                .orElseThrow(() -> new LocalSidService.NoSidMappingFoundException("No SID matching fnr=" + fnr));
        return Publishers.just(getCurrentFnrForSnr(currentSnr).map(currentFnr ->
                        new SidInfo.SidInfoBuilder().currentFnr(currentFnr).currentSnr(currentSnr).build())
                .orElse(null));
    }

    @Override
    public Publisher<SidInfo> lookupSnr(String snr, Optional<String> snapshot) {
        String currentFnr = getCurrentFnrForSnr(snr)
                .orElseThrow(() -> new LocalSidService.NoSidMappingFoundException("No SID matching snr=" + snr));
        return Publishers.just(getCurrentSnrForFnr(currentFnr).map(currentSnr ->
                        new SidInfo.SidInfoBuilder().currentFnr(currentFnr).currentSnr(currentSnr).build())
                .orElse(null));
    }

    private Optional<String> getCurrentSnrForFnr(String fnr) {
        return Optional.ofNullable(fnrToCurrentSnr.get(fnr));
    }

    private Optional<String> getCurrentFnrForSnr(String snr) {
        return Optional.ofNullable(snrToCurrentFnr.get(snr));
    }

    public static class NoSidMappingFoundException extends RuntimeException {
        public NoSidMappingFoundException(String message) {
            super(message);
        }
    }

}
