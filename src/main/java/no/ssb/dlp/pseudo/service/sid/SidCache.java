package no.ssb.dlp.pseudo.service.sid;


import jakarta.inject.Singleton;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Singleton
class SidCache {

    private final Map<String, SidItem> fnrToSid = new HashMap<>();
    private final Map<String, SidItem> snrToSid = new HashMap<>();

    private Instant lastUpdated = Instant.now();

    public void clearAll() {
        fnrToSid.clear();
        snrToSid.clear();
        lastUpdated = Instant.now();
    }

    public void register(SidItem sidItem) {
        fnrToSid.put(sidItem.getFnr(), sidItem);
        snrToSid.put(sidItem.getSnr(), sidItem);
        lastUpdated = Instant.now();
    }

    public Optional<String> getSidForFnr(String fnr) {
        SidItem sidItem = fnrToSid.get(fnr);
        return sidItem == null ? Optional.empty() : Optional.of(sidItem.getSnr());
    }

    public Optional<String> getFnrForSnr(String snr) {
        SidItem sidItem = snrToSid.get(snr);
        return sidItem == null ? Optional.empty() : Optional.of(sidItem.getFnr());
    }

    public int size() {
        return fnrToSid.size();
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }
}
