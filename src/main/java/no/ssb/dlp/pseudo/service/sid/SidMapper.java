package no.ssb.dlp.pseudo.service.sid;

import com.google.auto.service.AutoService;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import lombok.extern.slf4j.Slf4j;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncInput;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncOutput;
import no.ssb.dapla.dlp.pseudo.func.map.MapFuncConfig;
import no.ssb.dapla.dlp.pseudo.func.map.Mapper;
import no.ssb.dapla.dlp.pseudo.func.map.MappingNotFoundException;
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

    static final String NO_MATCHING_FNR = "No SID-mapping found for fnr %s";
    static final String NO_MATCHING_SNR = "No SID-mapping found for snr %s";
    static final String INCORRECT_MATCHING_FNR = "Incorrect SID-mapping for fnr %s. Mapping returned the original fnr!";
    static final String INCORRECT_MATCHING_SNR = "Incorrect SID-mapping for snr %s. Mapping returned the original snr!";

    public SidMapper() {
        sidService = Application.getContext().getBean(SidService.class);
        partitionSize = Application.getContext().getProperty("sid.mapper.partition.size", Integer.class,
                DEFAULT_PARTITION_SIZE);
    }

    private final Set<String> identifiers = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<String, ObservableSubscriber<Map<String, SidInfo>>> bulkRequest = new ConcurrentHashMap<>();

    @Override
    public void init(PseudoFuncInput input) {
        identifiers.add(input.value());
    }

    @Override
    public PseudoFuncOutput map(PseudoFuncInput input) {
        return mapTo(input.value(), true);
    }

    @Override
    public PseudoFuncOutput restore(PseudoFuncInput input) {
        return mapTo(input.value(), false);
    }

    private PseudoFuncOutput mapTo(String identifier, boolean isFnr) {
        if (identifier == null) {
            return PseudoFuncOutput.of(null);
        }
        // Execute the bulk request if necessary
        if (bulkRequest.isEmpty()) {
            // Split fnrs or snrs into chunks of BULK_SIZE
            for (List<String> bulkIdentifiers : Lists.partition(List.copyOf(identifiers), partitionSize)) {
                log.info("Execute SID-mapping bulk request");
                final ObservableSubscriber<Map<String, SidInfo>> subscriber;

                if (isFnr) {
                    subscriber = ObservableSubscriber.subscribe(
                            sidService.lookupFnr(bulkIdentifiers, getSnapshot()));
                } else {
                    subscriber = ObservableSubscriber.subscribe(
                            sidService.lookupSnr(bulkIdentifiers, getSnapshot()));
                }

                for (String id : bulkIdentifiers) {
                    bulkRequest.put(id, subscriber);
                }
            }
        }
        SidInfo result = bulkRequest.get(identifier).awaitResult()
                .orElseThrow(() -> new RuntimeException("SID service did not respond"))
                .get(identifier);

        PseudoFuncOutput output = createMappingLogsAndOutput(result, isFnr, identifier);
        output.addMetadata(MapFuncConfig.Param.SNAPSHOT_DATE, result.datasetExtractionSnapshotTime());
        return output;
    }


    private PseudoFuncOutput createMappingLogsAndOutput(SidInfo sidInfo, boolean isFnr, String identifier) {
        //Mapping for fnr
        if (isFnr) {
            if (sidInfo == null || sidInfo.snr() == null) {
                throw new MappingNotFoundException(String.format(NO_MATCHING_FNR, Redactor.redactFnr(identifier)));
            } else if (identifier.equals(sidInfo.snr())) {
                String message = String.format(INCORRECT_MATCHING_FNR, Redactor.redactFnr(identifier));
                log.warn(message);
                PseudoFuncOutput output = PseudoFuncOutput.of(sidInfo.snr());
                output.addWarning(message);
                return output;
            }
            return PseudoFuncOutput.of(sidInfo.snr());
        }
        //Mapping for snr
        else {
            if (sidInfo == null || sidInfo.fnr() == null) {
                throw new MappingNotFoundException(String.format(NO_MATCHING_SNR, Redactor.redactSnr(identifier)));
            } else if (identifier.equals(sidInfo.fnr())) {
                String message = String.format(INCORRECT_MATCHING_SNR, Redactor.redactSnr(identifier));
                log.warn(message);
                PseudoFuncOutput output = PseudoFuncOutput.of(sidInfo.fnr());
                output.addWarning(message);
                return output;
            }
            return PseudoFuncOutput.of(sidInfo.fnr());
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
                        String.join(", ", availableSnapshots.items())));
            }
            List<LocalDate> availableSnapshotDates = availableSnapshots.items().stream()
                    .map(snapshot -> {
                        try {
                            return LocalDate.from(formatter.parse(snapshot));
                        } catch (DateTimeParseException e) {
                            throw new RuntimeException("Invalid date format from SID service");
                        }
                    }).toList();
            if (availableSnapshotDates.stream().allMatch(requestedSnapshotDate::isBefore)) {
                throw new InvalidSidSnapshotDateException(String.format("Requested date is of an earlier date than all available SID dates. Valid dates are: %s",
                        String.join(", ", availableSnapshots.items())));
            }
        }
        this.config = config;
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