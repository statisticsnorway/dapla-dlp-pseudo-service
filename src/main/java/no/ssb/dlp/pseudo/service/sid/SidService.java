package no.ssb.dlp.pseudo.service.sid;

import org.reactivestreams.Publisher;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface SidService {
    Publisher<SidInfo> lookupFnr(String fnr, Optional<String> snapshot);

    Publisher<SidInfo> lookupSnr(String fnr, Optional<String> snapshot);
    Publisher<Map<String, SidInfo>> lookupFnr(Set<String> fnrList, Optional<String> snapshot);
}
