package no.ssb.dlp.pseudo.service.export;

import com.google.common.base.CharMatcher;
import com.google.common.base.Stopwatch;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.MediaType;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.annotation.Async;
import io.reactivex.Flowable;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import no.ssb.dapla.dataset.api.DatasetMeta;
import no.ssb.dapla.parquet.FieldInterceptor;
import no.ssb.dapla.storage.client.DatasetStorage;
import no.ssb.dapla.storage.client.backend.gcs.GoogleCloudStorageBackend;
import no.ssb.dlp.pseudo.core.FieldPseudoInterceptor;
import no.ssb.dlp.pseudo.core.func.PseudoFuncRule;
import no.ssb.dlp.pseudo.core.func.PseudoFuncs;
import no.ssb.dlp.pseudo.core.PseudoOperation;
import no.ssb.dlp.pseudo.core.file.Compression;
import no.ssb.dlp.pseudo.core.file.MoreMediaTypes;
import no.ssb.dlp.pseudo.core.map.RecordMapSerializerFactory;
import no.ssb.dlp.pseudo.core.util.Json;
import no.ssb.dlp.pseudo.core.util.PathJoiner;
import no.ssb.dlp.pseudo.core.util.Zips;
import no.ssb.dlp.pseudo.service.datasetmeta.DatasetMetaService;
import no.ssb.dlp.pseudo.service.pseudo.PseudoConfig;
import no.ssb.dlp.pseudo.service.pseudo.PseudoSecrets;

import javax.inject.Singleton;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.CharMatcher.anyOf;
import static com.google.common.base.CharMatcher.inRange;
import static no.ssb.dlp.pseudo.core.util.Zips.ZipOptions.zipOpts;

@RequiredArgsConstructor
@Singleton
@Slf4j
public class ExportService {

    private static final CharMatcher ALLOWED_FILENAME_CHARACTERS = inRange('a', 'z')
            .or(inRange('A', 'Z'))
            .or(inRange('0', '9'))
            .or(anyOf("_-"));

    private final ExportConfig exportConfig;
    private final DatasetStorage datasetClient;
    private final GoogleCloudStorageBackend storageBackend;
    private final PseudoSecrets pseudoSecrets;
    private final DatasetMetaService datasetMetaService;
    private final ApplicationEventPublisher<ExportEvent> eventPublisher;

    public DatasetExportResult export(DatasetExport e) {
        final DatasetExportReport report = new DatasetExportReport(e);
        final DatasetMeta sourceDatasetMeta = datasetMetaService.readDatasetMeta(e.getSourceDataset().legacyDatasetUri()).orElse(null);

        // Initialize target names
        if (e.getTargetContentName() == null || e.getTargetContentName().isBlank()) {
            // if not specified then deduce content name from dataset name
            e.setTargetContentName(e.getSourceDataset().getPath().replaceFirst(".*/([^/?]+).*", "$1"));
        }
        String targetRootLocation = targetRootLocationOf(e.getSourceDataset());
        String targetArchiveUri = targetFileLocationOf(targetRootLocation, archiveFilenameOf(e.getTargetContentName(), e.getCompression().getType()));

        eventPublisher.publishEvent(ExportEvent.builder()
                .targetRootLocation(targetRootLocation)
                .targetArchiveUri(targetArchiveUri)
                .datasetExport(e)
                .report(report)
                .sourceDataset(e.getSourceDataset())
                .sourceDatasetMeta(sourceDatasetMeta)
                .build());

        return DatasetExportResult.builder()
                .targetUri(targetArchiveUri)
                .build();
    }

    @EventListener
    @Async
    public void onExportEvent(ExportEvent e) {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        final AtomicInteger counter = new AtomicInteger();

        // Initialize depseudo mechanism if needed - else use a noOp interceptor
        FieldInterceptor fieldPseudoInterceptor = initPseudoInterceptor(e.datasetExport(), e.sourceDatasetMeta(), e.report());

        // Initiate record stream
        log.debug("Read parquet records");
        Flowable<Map<String, Object>> records = datasetClient.readParquetRecords(e.sourceDataset().legacyDatasetUri(), e.datasetExport().getColumnSelectors(), fieldPseudoInterceptor)
                .doOnNext(s -> {
                    if (counter.incrementAndGet() % 100000 == 0) {
                        log.debug(String.format("Processed %,d records", counter.get()));
                    }
                });

        // Serialize records
        MediaType targetContentType = MoreMediaTypes.validContentType(e.datasetExport().getTargetContentType());
        log.debug("Serialize records as %s".formatted(targetContentType));
        Flowable<String> serializedRecords = RecordMapSerializerFactory.newFromMediaType(targetContentType).serialize(records);

        // Encrypt and compress stream contents:
        Flowable<byte[]> compressedRecords = encryptAndCompress(e.datasetExport(), serializedRecords);
        // Upload stream contents

        storageBackend
                .write(e.targetArchiveUri(), compressedRecords)
                .timeout(30, TimeUnit.SECONDS)
                .doOnError(throwable -> log.error("Upload failed: %s".formatted(e.targetArchiveUri()), throwable))
                .subscribe(() -> {
                    e.report().setElapsedMillis(stopwatch.stop().elapsed(TimeUnit.MILLISECONDS));
                    log.info("Successful upload: %s".formatted(e.targetArchiveUri()));
                    uploadExportReport(e.report(), e.targetRootLocation());
                });
    }

    void uploadExportReport(DatasetExportReport report, String targetRootLocation) {
        String targetUri = targetFileLocationOf(targetRootLocation, ".export-meta.json");
        try {
            storageBackend.write(targetUri, Json.prettyFrom(report).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.error("Error uploading export report to " + targetUri, e);
        }
    }

    /**
     * Encrypt and compress serialized records to temporary file storage
     */
    Flowable<byte[]> encryptAndCompress(DatasetExport e, Flowable<String> serializedRecords) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        Zips.ZipOptions zipOptions = zipOpts()
                .password(e.getCompression().getPassword())
                .encryptionMethod(e.getCompression().getEncryption())
                .build();
        log.debug("Compress and encrypt serialized stream to temporary file. Encryption type: %s, Content name: %s. This can take some time...".formatted(e.getCompression().getEncryption(), e.getTargetContentName()));
        Flowable<byte[]> compressedRecords = Zips.zip(serializedRecords, archiveFilenameOf(e.getTargetContentName(), e.getTargetContentType()), zipOptions);
        log.debug("Compression/encryption done in %s".formatted(stopwatch.stop().elapsed()));
        return compressedRecords;
    }

    String targetRootLocationOf(DatasetUri datasetPath) {
        return PathJoiner.joinWithoutLeadingOrTrailingSlash(
                exportConfig.getDefaultTargetRoot(),
                datasetPath.getPath(),
                "" + System.currentTimeMillis()
        );
    }

    String targetFileLocationOf(String rootLocation, String filename) {
        return PathJoiner.joinWithoutLeadingOrTrailingSlash(
                rootLocation,
                filename
        );
    }

    String archiveFilenameOf(String contentName, MediaType contentType) {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());
        contentName = ALLOWED_FILENAME_CHARACTERS.retainFrom(contentName);

        return "%s-%s.%s".formatted(timestamp, contentName, contentType.getExtension().toLowerCase());
    }

    FieldInterceptor initPseudoInterceptor(DatasetExport e, DatasetMeta datasetMeta, DatasetExportReport report) {
        if (!e.getDepseudonymize()) {
            return FieldInterceptor.noOp();
        }

        final List<PseudoFuncRule> pseudoRules;

        // Use any explicitly specified pseudo rules
        if (e.getPseudoRules() != null && !e.getPseudoRules().isEmpty()) {
            log.debug("Pseudo rules were explicitly specified");
            pseudoRules = e.getPseudoRules();
        }

        // Or try to retrieve pseudo rules from another dataset's metadata file if this has been specified
        else if (e.getPseudoRulesDataset() != null) {
            log.debug("Retrieve pseudo rules from explicitly specified dataset path {}", e.getPseudoRulesDataset());
            PseudoConfig pseudoConfig = datasetMetaService.readDatasetPseudoConfig(e.getPseudoRulesDataset().legacyDatasetUri());
            pseudoRules = pseudoConfig.getRules();
        }
        // Or try to retrieve the pseudo rules from the source dataset's metadata (if present)
        else if (e.getSourceDataset() != null) {
            log.debug("Retrieve pseudo rules from source dataset at {}", e.getSourceDataset());
            PseudoConfig pseudoConfig = datasetMetaService.pseudoConfigOf(datasetMeta);
            pseudoRules = pseudoConfig.getRules();
        }

        // Else proceed without any pseudo rules
        else {
            pseudoRules = List.of();
        }

        if (e.getDepseudonymize() && pseudoRules.isEmpty()) {
            throw new NoPseudoRulesFoundException("no pseudonymization rules found - unable to depseudonymize dataset %s".formatted(e.getSourceDataset()));
        } else {
            log.info("Pseudo rules: {}", pseudoRules);
        }

        report.setAppliedPseudoRules(pseudoRules);
        PseudoFuncs pseudoFuncs = new PseudoFuncs(pseudoRules, pseudoSecrets.resolve(), List.of());
        return new FieldPseudoInterceptor(pseudoFuncs, PseudoOperation.DEPSEUDONYMIZE);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatasetUri {
        @NonNull
        private String root;
        @NonNull
        private String path;
        @NonNull
        private String version;

        no.ssb.dapla.dataset.uri.DatasetUri legacyDatasetUri() {
            return no.ssb.dapla.dataset.uri.DatasetUri.of(root, path, version);
        }
    }

    @Data
    @Builder
    @Introspected
    static class DatasetExport {
        private String userId;

        @NotNull
        private DatasetUri sourceDataset;
        private Set<String> columnSelectors;

        @NotNull
        private Compression compression;

        private Boolean depseudonymize;

        private List<PseudoFuncRule> pseudoRules;
        private DatasetUri pseudoRulesDataset;

        private String targetContentName;
        private MediaType targetContentType;
    }

    @Value
    @Builder
    @Accessors(fluent = true)
    public static class ExportEvent {
        @NonNull DatasetUri sourceDataset;
        DatasetMeta sourceDatasetMeta;
        @NonNull DatasetExport datasetExport;
        @NonNull DatasetExportReport report;
        @NonNull String targetRootLocation;
        @NonNull String targetArchiveUri;
    }

    @Data
    @Builder
    static class DatasetExportResult {
        private String targetUri;
    }

    @Data
    static class DatasetExportReport {
        public DatasetExportReport(DatasetExport e) {
            this.userId = e.getUserId();
            this.exportTimestamp = Instant.now().toString();
            this.depseudonymize = e.getDepseudonymize();
            this.sourceDataset = e.getSourceDataset();
        }

        private final DatasetUri sourceDataset;
        private final String userId;
        private final String exportTimestamp;
        private Long elapsedMillis;
        private boolean depseudonymize;
        private List<PseudoFuncRule> appliedPseudoRules;
    }

    public static class ExportServiceException extends RuntimeException {
        public ExportServiceException(String message) {
            super(message);
        }
    }

    public static class DatasetNotFoundException extends ExportServiceException {
        public DatasetNotFoundException(String message) {
            super(message);
        }
    }

    public static class NoPseudoRulesFoundException extends ExportServiceException {
        public NoPseudoRulesFoundException(String message) {
            super(message);
        }
    }

}
