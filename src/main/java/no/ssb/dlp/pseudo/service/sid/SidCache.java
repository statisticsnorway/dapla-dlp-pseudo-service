package no.ssb.dlp.pseudo.service.sid;


import jakarta.inject.Singleton;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Singleton
class SidCache {

    private final Map<String, SidItem> fnrToSidItem = new HashMap<>();
    private final Map<String, SidItem> snrToSidItem = new HashMap<>();

    private Instant lastUpdated = Instant.now();
    private State state = State.NOT_INITIALIZED;

    public void clearAll() {
        fnrToSidItem.clear();
        snrToSidItem.clear();
        lastUpdated = Instant.now();
    }

    public void register(SidItem sidItem) {
        register(sidItem, false);
    }

    void register(SidItem sidItem, boolean hasMoreUpdates) {
        fnrToSidItem.put(sidItem.getFnr(), sidItem);
        snrToSidItem.put(sidItem.getSnr(), sidItem);

        if (! hasMoreUpdates) {
            this.lastUpdated = Instant.now();
        }
    }

    void markAsInitialized() {
        lastUpdated = Instant.now();
        lastUpdated = Instant.now();
    }

    public Optional<SidItem> getSidItemForFnr(String fnr) {
        return Optional.ofNullable(fnrToSidItem.get(fnr));
    }

    public Optional<String> getSidForFnr(String fnr) {
        return getSidItemForFnr(fnr)
                .map(s -> s.getCurrentSnr());
    }

    public Optional<SidItem> getSidItemForSnr(String snr) {
        return Optional.ofNullable(snrToSidItem.get(snr));
    }

    public Optional<String> getSidForSnr(String sid) {
        return getSidItemForSnr(sid)
                .map(s -> s.getSnr());
    }

    public int size() {
        return fnrToSidItem.size();
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public State getState() {
        return state;
    }

    public enum State {
        NOT_INITIALIZED, INITIALIZED;
    }
}
