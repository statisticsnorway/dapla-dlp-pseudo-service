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
        state = State.REINITIALIZING;
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
        state = State.INITIALIZED;
    }

    public Optional<SidItem> getSidItemForFnr(String fnr) {
        SidItem sidItem = fnrToSidItem.get(fnr);
        if (sidItem == null) {
            validateCacheReady();
        }
        return Optional.ofNullable(sidItem);
    }

    public Optional<String> getSidForFnr(String fnr) {
        return getSidItemForFnr(fnr)
                .map(s -> s.getCurrentSnr());
    }

    public Optional<SidItem> getSidItemForSnr(String snr) {
        SidItem sidItem = snrToSidItem.get(snr);
        if (sidItem == null) {
            validateCacheReady();
        }
        return Optional.ofNullable(sidItem);
    }

    public Optional<String> getSidForSnr(String sid) {
        return getSidItemForSnr(sid)
                .map(s -> s.getCurrentSnr());
    }

    private void validateCacheReady() throws SidIndexUnavailableException {
        if (state != State.INITIALIZED) {
            throw new SidIndexUnavailableException("SID index is not currently available. Wait a minute and retry. State=" + state);
        }
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
        NOT_INITIALIZED, REINITIALIZING, INITIALIZED;
    }

}
