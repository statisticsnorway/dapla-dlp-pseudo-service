package no.ssb.dlp.pseudo.service.sid;

import com.google.common.base.Stopwatch;
import io.micronaut.context.annotation.Property;
import io.micronaut.objectstorage.googlecloud.GoogleCloudStorageEntry;
import io.micronaut.objectstorage.googlecloud.GoogleCloudStorageOperations;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import io.micronaut.scheduling.annotation.Async;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

@Singleton
@Slf4j
public class SidCacheLoader {
    private final SidReader sidReader;
    private final String sidFile;

    @Getter
    private final SidCache sidCache;

    @Named("sid")
    private final GoogleCloudStorageOperations objectStorage;

    public SidCacheLoader(SidReader sidReader, @Property(name = "sid.filename") String sidFile, GoogleCloudStorageOperations objectStorage, SidCache sidCache) {
        this.sidReader = sidReader;
        this.sidFile = sidFile;
        this.objectStorage = objectStorage;
        this.sidCache = sidCache;
    }

    @EventListener
    @Async
    public void loadSidData(final ServerStartupEvent event) {
        reloadSidData();
    }

    public void reloadSidData() {
        log.info("Load SID data from GCS...");
        GoogleCloudStorageEntry item = objectStorage.retrieve(sidFile)
                .orElseThrow(() -> new SidCacheInitException("Unable to read SID mappings from " + sidFile));

        Stopwatch stopwatch = Stopwatch.createStarted();
        sidCache.clearAll();
        sidReader.readSidsFromFile(item.getInputStream(), sidCache);
        log.info("Read %s sid mappings in %s".formatted(sidCache.size(), stopwatch.stop().elapsed()));
    }

    public String getSource() {
        return sidFile;
    }

    public static class SidCacheInitException extends RuntimeException {
        public SidCacheInitException(String message) {
            super(message);
        }

        public SidCacheInitException(String message, Throwable cause) {
            super(message, cause);
        }
    }


}
