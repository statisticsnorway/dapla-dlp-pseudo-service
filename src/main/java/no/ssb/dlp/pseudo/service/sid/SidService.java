package no.ssb.dlp.pseudo.service.sid;

import org.reactivestreams.Publisher;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface SidService {
    Publisher<SidInfo> lookupFnr(String fnr, Optional<String> snapshot);

    Publisher<SidInfo> lookupSnr(String fnr, Optional<String> snapshot);
    Publisher<Map<String, SidInfo>> lookupFnr(List<String> fnrList, Optional<String> snapshot);

    Publisher<MultiSidLookupResponse> lookupMissing(List<String> fnrList, Optional<String> snapshot);

    Publisher<SnapshotInfo> getSnapshots();
}
