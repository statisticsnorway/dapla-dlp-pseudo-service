package no.ssb.dlp.pseudo.service.sid.local;

import com.google.common.base.Stopwatch;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requirements;
import io.micronaut.context.annotation.Requires;
import io.micronaut.objectstorage.googlecloud.GoogleCloudStorageEntry;
import io.micronaut.objectstorage.googlecloud.GoogleCloudStorageOperations;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import io.micronaut.scheduling.annotation.Async;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.time.DurationFormatUtils.formatDurationWords;

@Singleton
@Slf4j
@Requirements({
        @Requires(env = "local-sid"),
        @Requires(property = "micronaut.object-storage.gcp.sid.bucket")
})
public class SidCacheLoader {
    private final SidReader sidReader;
    private final String sidFile;
    @Getter
    private final SidCache sidCache;

    private final GoogleCloudStorageOperations objectStorage;

    // TODO: Check if we can omit this explicit constructor by annotating the fields instead?
    public SidCacheLoader(SidReader sidReader,
                          @Property(name = "sid.mapping.filename") String sidFile,
                          @Named("sid") GoogleCloudStorageOperations objectStorage,
                          SidCache sidCache) {
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

        sidReader.readSidsFromFile(item.getInputStream()).subscribe(
                // onNext
                sidItem -> sidCache.register(sidItem, true)
                ,

                // onError
                e -> {
                    throw new SidCacheInitException("Unable to read SID mappings from " + sidFile, e);
                },

                // onComplete
                sidCache::markAsInitialized
        );

        log.info("Read %s sid mappings in %s".formatted(
                sidCache.size(),
                formatDurationWords(stopwatch.elapsed(TimeUnit.MILLISECONDS), true, true))
        );
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
