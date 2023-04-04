package no.ssb.dlp.pseudo.service.sid;

import org.reactivestreams.Publisher;

import java.util.Optional;

public interface SidService {
    Publisher<SidInfo> lookupFnr(String fnr, Optional<String> snapshot);

    Publisher<SidInfo> lookupSnr(String snr, Optional<String> snapshot);
}
