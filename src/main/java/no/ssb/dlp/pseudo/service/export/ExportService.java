package no.ssb.dlp.pseudo.service.export;

import com.google.common.base.CharMatcher;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.MediaType;
import io.reactivex.Flowable;
import io.reactivex.Single;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.ssb.dapla.dataset.uri.DatasetUri;
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
import no.ssb.dlp.pseudo.service.datasetmeta.DatasetMetaService;
import no.ssb.dlp.pseudo.service.pseudo.PseudoConfig;
import no.ssb.dlp.pseudo.service.pseudo.PseudoSecrets;

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

    @Data
    @Builder
    @Introspected
    static class DatasetExport {
        private DatasetUri sourceDatasetUri;
        private Set<String> columnSelectors;

        @NotNull
        private Compression compression;
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
        FieldPseudoInterceptor fieldPseudoInterceptor = initPseudoInterceptor(e.getPseudoConfig(), e.getSourceDatasetUri());

        String targetName = Optional.ofNullable(e.getTargetName())
          .orElse(e.getSourceDatasetUri().getPath().replaceFirst(".*/([^/?]+).*", "$1"));
        MediaType targetContentType = MoreMediaTypes.validContentType(e.getTargetContentType());

        // Initiate record stream
        Flowable<Map<String, Object>> records = datasetClient.readParquetRecords(e.getSourceDatasetUri(), e.getColumnSelectors(), fieldPseudoInterceptor);

        // Serialize records
        Flowable<String> serializedRecords = RecordMapSerializerFactory.newFromMediaType(targetContentType).serialize(records);

        Zips.ZipOptions zipOptions = zipOpts()
          .password(e.getCompression().getPassword())
          .encryptionMethod(e.getCompression().getEncryption())
          .build();

        // Compress and encrypt stream contents
        Flowable<byte[]> compressedRecords = Zips.zip(serializedRecords, filenameOf(targetName, e.getTargetContentType()), zipOptions);

        String targetUri = PathJoiner.joinWithoutLeadingOrTrailingSlash(e.getTargetPath(), filenameOf(targetName, e.getCompression().getType()));

        // Upload stream contents
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

    String filenameOf(String contentName, MediaType contentType) {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd")
          .withZone(ZoneOffset.UTC)
          .format(Instant.now());
        contentName = ALLOWED_FILENAME_CHARACTERS.retainFrom(contentName);

        return "%s-%s.%s".formatted(timestamp, contentName, contentType.getExtension().toLowerCase());
    }

    FieldPseudoInterceptor initPseudoInterceptor(PseudoConfig pseudoConfig, DatasetUri datasetUri) {
        if (pseudoConfig == null || pseudoConfig.getRules().isEmpty()) {
            log.info("No explicit pseudo config provided. Checking for pseudo config in .dataset-meta.json");
            pseudoConfig = datasetMetaService.readDatasetPseudoConfig(datasetUri);
        }
        List<PseudoFuncRule> pseudoRules = pseudoConfig.getRules();

        log.info(pseudoRules.isEmpty()
          ? "No pseudo config provided. Target dataset will not be depseudonymized"
          : "Pseudo rules " + pseudoRules);

        PseudoFuncs pseudoFuncs = new PseudoFuncs(pseudoConfig.getRules(), pseudoSecrets.resolve());
        return new FieldPseudoInterceptor(pseudoFuncs, PseudoOperation.DEPSEUDONYMIZE);
    }
}
