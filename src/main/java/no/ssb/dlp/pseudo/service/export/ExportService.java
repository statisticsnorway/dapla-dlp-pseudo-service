package no.ssb.dlp.pseudo.service.export;

import com.google.common.base.CharMatcher;
import com.google.common.base.Stopwatch;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.MediaType;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.reactivex.Flowable;
import io.reactivex.Single;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.ssb.dapla.dataset.api.DatasetId;
import no.ssb.dapla.dataset.uri.DatasetUri;
import no.ssb.dapla.parquet.FieldInterceptor;
import no.ssb.dapla.storage.client.DatasetStorage;
import no.ssb.dapla.storage.client.backend.gcs.GoogleCloudStorageBackend;
import no.ssb.dlp.pseudo.core.FieldPseudoInterceptor;
import no.ssb.dlp.pseudo.core.PseudoFuncRule;
import no.ssb.dlp.pseudo.core.PseudoFuncs;
import no.ssb.dlp.pseudo.core.PseudoOperation;
import no.ssb.dlp.pseudo.core.file.Compression;
import no.ssb.dlp.pseudo.core.file.MoreMediaTypes;
import no.ssb.dlp.pseudo.core.map.RecordMapSerializerFactory;
import no.ssb.dlp.pseudo.core.util.PathJoiner;
import no.ssb.dlp.pseudo.core.util.Zips;
import no.ssb.dlp.pseudo.service.catalog.CatalogService;
import no.ssb.dlp.pseudo.service.datasetmeta.DatasetMetaService;
import no.ssb.dlp.pseudo.service.pseudo.PseudoConfig;
import no.ssb.dlp.pseudo.service.pseudo.PseudoSecrets;
import no.ssb.dlp.pseudo.service.useraccess.UserAccessService;
import no.ssb.dlp.pseudo.service.useraccess.UserAccessService.AccessCheckRequest;

import javax.inject.Singleton;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.CharMatcher.anyOf;
import static com.google.common.base.CharMatcher.inRange;
import static no.ssb.dlp.pseudo.core.util.Zips.ZipOptions.zipOpts;
import static no.ssb.dlp.pseudo.service.catalog.CatalogService.GetDatasetRequest;

@RequiredArgsConstructor
@Singleton
@Slf4j
public class ExportService {

    private final static CharMatcher ALLOWED_FILENAME_CHARACTERS = inRange('a', 'z')
      .or(inRange('A', 'Z'))
      .or(inRange('0', '9'))
      .or(anyOf("_-"));

    private final DatasetStorage datasetClient;
    private final GoogleCloudStorageBackend storageBackend;
    private final PseudoSecrets pseudoSecrets;
    private final DatasetMetaService datasetMetaService;
    private final CatalogService catalogService;
    private final UserAccessService userAccessService;

    @Data
    @Builder
    @Introspected
    static class DatasetExport {
        private String userId;

        @NotNull
        private DatasetId sourceDatasetId;
        private Set<String> columnSelectors;

        @NotNull
        private Compression compression;

        private Boolean depseudonymize;
        private PseudoConfig pseudoConfig;

        private String targetPath;
        private String targetName;
        private MediaType targetContentType;
    }

    @Data
    @Builder
    static class DatasetExportResult {
        private String targetUri;
    }

    public Single<DatasetExportResult> export(DatasetExport e) {
        // Retrieve dataset information from the catalog service
        CatalogService.Dataset datasetInfo = getDatasetInfo(e.getSourceDatasetId()).orElseThrow(
          () -> new ExportServiceException("Unable to resolve dataset '%s' in catalog".formatted(e.getSourceDatasetId().getPath()))
        );

        // Check if the user has access to export the dataset
        UserAccessService.DatasetPrivilege accessPrivilege = e.getDepseudonymize()
          ? UserAccessService.DatasetPrivilege.DEPSEUDO
          : UserAccessService.DatasetPrivilege.READ;
        if (! hasAccess(e.getUserId(), accessPrivilege, datasetInfo)) {
            throw new ExportServiceException("User %s does not have %s access to %s".formatted(e.getUserId(), accessPrivilege, datasetInfo.getId().getPath()));
        }

        // Initialize depseudo mechanism if needed - else use a noOp interceptor
        FieldInterceptor fieldPseudoInterceptor = e.getDepseudonymize()
          ? initPseudoInterceptor(e.getPseudoConfig(), datasetInfo.datasetUri())
          : FieldInterceptor.noOp();

        // Initiate record stream
        log.debug("Read parquet records");
        Flowable<Map<String, Object>> records = datasetClient.readParquetRecords(datasetInfo.datasetUri(), e.getColumnSelectors(), fieldPseudoInterceptor);

        // Serialize records
        MediaType targetContentType = MoreMediaTypes.validContentType(e.getTargetContentType());
        log.debug("Serialize records as %s".formatted(targetContentType));
        Flowable<String> serializedRecords = RecordMapSerializerFactory.newFromMediaType(targetContentType).serialize(records);

        // Encrypt and compress stream contents
        if (e.getTargetName() == null) {
            e.setTargetName(e.getSourceDatasetId().getPath().replaceFirst(".*/([^/?]+).*", "$1"));
        }
        Flowable<byte[]> compressedRecords = encryptAndCompress(e, serializedRecords);

        // Upload stream contents
        String targetUri = PathJoiner.joinWithoutLeadingOrTrailingSlash(e.getTargetPath(), filenameOf(e.getTargetName(), e.getCompression().getType()));
        log.debug("Uploading results to %s".formatted(targetUri));
        return storageBackend
          .write(targetUri, compressedRecords)
          .timeout(30, TimeUnit.SECONDS)
          .doOnError(throwable -> log.error("Upload failed: %s".formatted(targetUri), throwable))
          .toSingle(() -> {
              log.info("Successful upload: %s".formatted(targetUri));
              return DatasetExportResult.builder()
                .targetUri(targetUri)
                .build();
          });
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
        log.debug("Compress and encrypt serialized stream to temporary file. Encryption type: %s, Content name: %s. This can take some time...".formatted(e.getCompression().getEncryption(), e.getTargetName()));
        Flowable<byte[]> compressedRecords = Zips.zip(serializedRecords, filenameOf(e.getTargetName(), e.getTargetContentType()), zipOptions);
        log.debug("Compression/encryption done in %s" .formatted(stopwatch.elapsed()));
        return compressedRecords;
    }

    /**
     * Retrieve information about a dataset from the catalog service
     *
     * @param datasetId with path and closest dataset timestamp
     * @return dataset info including bucket name, path, version, valuation, etc
     */
    Optional<CatalogService.Dataset> getDatasetInfo(DatasetId datasetId) {
        GetDatasetRequest request = GetDatasetRequest.builder()
          .path(datasetId.getPath())
          .timestamp(datasetId.getVersion())
          .build();
        log.debug("getDatasetInfo %s".formatted(request));
        CatalogService.Dataset datasetInfo = catalogService.getDataset(request)
          .blockingGet()
          .getDataset();
        log.debug("datasetInfo %s".formatted(datasetInfo));
        return Optional.ofNullable( datasetInfo);
    }

    /**
     * Check if a given user has a specific privileged access to a given dataset
     *
     * @param userId e.g. firstname.lastname@ssb.no
     * @param accessPrivilege CREATE, READ, UPDATE, DELETE, DEPSEUDO
     * @param datasetInfo dataset info retrieved from the catalog service
     * @return true iff the user has the conditional access
     */
    boolean hasAccess(String userId, UserAccessService.DatasetPrivilege accessPrivilege, CatalogService.Dataset datasetInfo) {
        AccessCheckRequest request = AccessCheckRequest.builder()
          .userId(userId)
          .privilege(accessPrivilege)
          .path(datasetInfo.getId().getPath())
          .state(datasetInfo.getState())
          .valuation(datasetInfo.getValuation())
          .build();
        log.debug("Check access %s".formatted(request));
        boolean hasAccess = userAccessService.hasAccess(request).blockingGet();
        log.debug("hasAccess: %s".formatted(hasAccess));
        return hasAccess;
    }

    String filenameOf(String contentName, MediaType contentType) {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd")
          .withZone(ZoneOffset.UTC)
          .format(Instant.now());
        contentName = ALLOWED_FILENAME_CHARACTERS.retainFrom(contentName);

        return "%s-%s.%s".formatted(timestamp, contentName, contentType.getExtension().toLowerCase());
    }

    FieldPseudoInterceptor initPseudoInterceptor(PseudoConfig pseudoConfig, DatasetUri datasetUri) {
        log.debug("Initializing depseudonymization mechanism");
        if (pseudoConfig == null || pseudoConfig.getRules().isEmpty()) {
            log.info("No explicit pseudo config provided. Checking for pseudo config in .dataset-meta.json");
            pseudoConfig = datasetMetaService.readDatasetPseudoConfig(datasetUri);
        }
        List<PseudoFuncRule> pseudoRules = pseudoConfig.getRules();

        // TODO: Fail if no pseudo rules could be resolved?
        log.info(pseudoRules.isEmpty()
          ? "No pseudo config provided. Target dataset will not be depseudonymized"
          : "Pseudo rules " + pseudoRules);

        PseudoFuncs pseudoFuncs = new PseudoFuncs(pseudoConfig.getRules(), pseudoSecrets.resolve());
        return new FieldPseudoInterceptor(pseudoFuncs, PseudoOperation.DEPSEUDONYMIZE);
    }

    class ExportServiceException extends RuntimeException {
        public ExportServiceException(String message) {
            super(message);
        }
    }

}
