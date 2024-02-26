package no.ssb.dlp.pseudo.service.sid;

import com.google.auto.service.AutoService;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import lombok.extern.slf4j.Slf4j;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncInput;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncOutput;
import no.ssb.dapla.dlp.pseudo.func.map.MapFuncConfig;
import no.ssb.dapla.dlp.pseudo.func.map.Mapper;
import no.ssb.dlp.pseudo.service.Application;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Service Provider class that implements the {@link Mapper} pseudo function. This class will be invoked by the JDK's
 * Service Loader mechanism for {@link no.ssb.dlp.pseudo.core.func.PseudoFuncNames#MAP_SID} pseudo functions.
 */
@AutoService(Mapper.class)
@Slf4j
public class SidMapper implements Mapper {

    private final SidService sidService;

    private static final int DEFAULT_PARTITION_SIZE = 50000;
    private final int partitionSize;
    private Map<String, Object> config = Collections.emptyMap();

    public SidMapper() {
        sidService = Application.getContext().getBean(SidService.class);
        partitionSize = Application.getContext().getProperty("sid.mapper.partition.size", Integer.class,
                DEFAULT_PARTITION_SIZE);
    }

    private final Set<String> fnrs = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<String, ObservableSubscriber<Map<String, SidInfo>>> bulkRequest = new ConcurrentHashMap<>();

    @Override
    public void init(PseudoFuncInput input) {
        for (Object inputValue : input.getValues()) {
            fnrs.add(String.valueOf(inputValue));
        }
    }

    @Override
    public PseudoFuncOutput map(PseudoFuncInput input) {
        PseudoFuncOutput output = new PseudoFuncOutput();
        for (Object inputValue : input.getValues()) {
            String plain = String.valueOf(inputValue);
            mapTo(plain, output);
        }
        return output;
    }

    private void mapTo(String fnr, PseudoFuncOutput output) {
        if (fnr == null) {
            return;
        }
        try {
            // Execute the bulk request if necessary
            if (bulkRequest.isEmpty()) {
                // Split fnrs into chunks of BULK_SIZE
                for (List<String> bulkFnr: Lists.partition(List.copyOf(fnrs), partitionSize)) {
                    log.info("Execute SID-mapping bulk request");
                    final ObservableSubscriber<Map<String, SidInfo>> subscriber = ObservableSubscriber.subscribe(
                            sidService.lookupFnr(bulkFnr, getSnapshot()));
                    for (String f: bulkFnr) {
                        bulkRequest.put(f, subscriber);
                    }
                }
            }
            SidInfo result = bulkRequest.get(fnr).awaitResult()
                    .orElseThrow(() -> new RuntimeException("SID service did not respond")).get(fnr);
            if (result == null) {
                log.warn("No SID-mapping found for fnr starting with {}", Strings.padEnd(fnr, 6, ' ').substring(0, 6));
                output.addWarning(String.format("No SID-mapping found for fnr %s", fnr));
                output.add(fnr);
            } else if (result.snr() == null) {
                log.warn("No SID-mapping found for fnr starting with {}", Strings.padEnd(fnr, 6, ' ').substring(0, 6));
                output.addMetadata(MapFuncConfig.Param.SNAPSHOT_DATE, result.datasetExtractionSnapshotTime());
                output.addWarning(String.format("No SID-mapping found for fnr %s", fnr));
                output.add(fnr);
            } else {
                if (fnr.equals(result.snr())) {
                    log.warn("Incorrect SID-mapping for fnr starting with {}. Mapping returned the original fnr!",
                            Strings.padEnd(fnr, 6, ' ').substring(0, 6));
                    output.addWarning(String.format("Incorrect SID-mapping for fnr %s. Mapping returned the original fnr!", fnr));
                } else {
                    log.debug("Successfully mapped fnr starting with {}", Strings.padEnd(fnr, 6, ' ').substring(0, 6));
                }
                output.addMetadata(MapFuncConfig.Param.SNAPSHOT_DATE, result.datasetExtractionSnapshotTime());
                output.add(result.snr());
            }
        } catch (LocalSidService.NoSidMappingFoundException e) {
            log.warn("No SID-mapping found for fnr starting with {}", Strings.padEnd(fnr, 6, ' ').substring(0, 6));
            output.addWarning(String.format("No SID-mapping found for fnr %s", fnr));
            output.add(fnr);
        }
    }

    private Optional<String> getSnapshot() {
        return Optional.ofNullable(
                this.config.getOrDefault(MapFuncConfig.Param.SNAPSHOT_DATE, null)
        ).map(String::valueOf);
    }

        @Override
    public void setConfig(Map<String, Object> config) {
        if (config.containsKey(MapFuncConfig.Param.SNAPSHOT_DATE)) {
            SnapshotInfo availableSnapshots = ObservableSubscriber.subscribe(this.sidService.getSnapshots()).awaitResult()
                    .orElseThrow(() -> new RuntimeException("SID service did not respond"));
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
            LocalDate requestedSnapshotDate;
            try { // Convert the SID snapshot in the request to Date format
                requestedSnapshotDate = LocalDate.from(formatter.
                        parse(config.get(MapFuncConfig.Param.SNAPSHOT_DATE).toString()));
            } catch (DateTimeParseException e) {
                throw new InvalidSidSnapshotDateException(String.format("Invalid snapshot date format. Valid dates are: %s",
                        String.join(", ", availableSnapshots.getItems())));
            }
            List<LocalDate> availableSnapshotDates = availableSnapshots.getItems().stream()
                    .map(snapshot -> {
                        try {
                            return LocalDate.from(formatter.parse(snapshot));
                        } catch (DateTimeParseException e) {
                            throw new RuntimeException("Invalid date format from SID service");
                        }
                    }).toList();
            if(availableSnapshotDates.stream().allMatch(requestedSnapshotDate::isBefore)){
                throw new InvalidSidSnapshotDateException(String.format("Requested date is of an earlier date than all available SID dates. Valid dates are: %s",
                        String.join(", ", availableSnapshots.getItems())));
            }
        }
        this.config = config;
    }

    @Override
    public PseudoFuncOutput restore(PseudoFuncInput input) {
        return PseudoFuncOutput.of(input.getValues());
    }

    /**
     * A Subscriber that stores the publishers results and provides a latch so can block on completion.
     *
     * @param <T> The publishers result type
     */
    static class ObservableSubscriber<T> implements Subscriber<T> {
        private final CountDownLatch latch = new CountDownLatch(1);
        private final Stopwatch stopwatch = Stopwatch.createUnstarted();

        private volatile T result;
        private volatile Subscription subscription;

        public static <T> ObservableSubscriber<T> subscribe(Publisher<T> publisher) {
            ObservableSubscriber<T> instance = new ObservableSubscriber<T>();
            publisher.subscribe(instance);
            return instance;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            this.stopwatch.start();
            this.subscription = subscription;
        }

        @Override
        public void onNext(T sidInfo) {
            result = sidInfo;
        }

        @Override
        public void onError(Throwable throwable) {
            if (throwable instanceof HttpClientResponseException exception) {
                if (exception.getStatus() == HttpStatus.NOT_FOUND) {
                    // This may happen more frequently, so log at debug level
                    log.debug("Error was", exception);
                } else {
                    log.warn("Unexpected error", exception);
                }
            } else {
                log.warn("Unexpected error", throwable);
            }
            onComplete();
        }

        @Override
        public void onComplete() {
            latch.countDown();
            log.info("Thread completed after {} seconds", stopwatch.stop().elapsed(TimeUnit.SECONDS));
        }

        public Optional<T> awaitResult() {
            return Optional.ofNullable(await().result);
        }

        private ObservableSubscriber<T> await() {
            subscription.request(1);
            try {
                if (!latch.await(120, TimeUnit.SECONDS)) {
                    log.error("Publisher onComplete timed out");
                    throw new RuntimeException("Publisher onComplete timed out");
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return this;
        }
    }
}