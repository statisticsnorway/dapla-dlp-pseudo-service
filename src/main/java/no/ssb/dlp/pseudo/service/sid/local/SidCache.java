package no.ssb.dlp.pseudo.service.sid.local;


import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import no.ssb.dlp.pseudo.service.sid.SidIndexUnavailableException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Singleton
@Requires(env = "local-sid")
public class SidCache {

    private final Map<String, String> fnrToCurrentSnr = new HashMap<>();
    private final Map<String, String> snrToCurrentFnr = new HashMap<>();

    private Instant lastUpdated;
    private State state = State.NOT_INITIALIZED;

    public void clearAll() {
        fnrToCurrentSnr.clear();
        snrToCurrentFnr.clear();
        state = State.NOT_INITIALIZED;
        lastUpdated = Instant.now();
    }

    void register(SidItem sidItem, boolean hasMoreUpdates) {
        fnrToCurrentSnr.put(sidItem.getFnr(), sidItem.getCurrentSnr());
        snrToCurrentFnr.put(sidItem.getSnr(), sidItem.getCurrentFnr());

        if (! hasMoreUpdates) {
            markAsInitialized();
        }
    }

    void markAsInitialized() {
        lastUpdated = Instant.now();
        state = State.INITIALIZED;
    }

    public Optional<String> getCurrentSnrForFnr(String fnr) {
        String currentSnr = fnrToCurrentSnr.get(fnr);
        if (currentSnr == null) {
            validateCacheReady();
        }

        return Optional.ofNullable(currentSnr);
    }

    public Optional<String> getCurrentFnrForSnr(String snr) {
        String currentFnr = snrToCurrentFnr.get(snr);
        if (currentFnr == null) {
            validateCacheReady();
        }

        return Optional.ofNullable(currentFnr);
    }
    public List<String> getCurrentSnrList(List<String> fnrs) {
        return fnrToCurrentSnr.entrySet().stream()
                .filter(entry -> fnrs.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();
    }
    public List<String> getCurrentFnrList(List<String> snrs) {
        return snrToCurrentFnr.entrySet().stream()
                .filter(entry -> snrs.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();
    }
    private void validateCacheReady() throws SidIndexUnavailableException {
        if (state != State.INITIALIZED) {
            throw new SidIndexUnavailableException("SID index is not currently available. Wait a minute and retry. State=" + state);
        }
    }

    public int size() {
        return fnrToCurrentSnr.size();
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
