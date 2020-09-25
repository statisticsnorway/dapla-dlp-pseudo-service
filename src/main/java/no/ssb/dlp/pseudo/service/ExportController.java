package no.ssb.dlp.pseudo.service;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.reactivex.Flowable;
import io.reactivex.Single;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.ssb.dapla.dataset.uri.DatasetUri;
import no.ssb.dapla.storage.client.DatasetStorage;
import no.ssb.dapla.storage.client.backend.gcs.GoogleCloudStorageBackend;
import no.ssb.dlp.pseudo.service.mediatype.Compression;
import no.ssb.dlp.pseudo.service.mediatype.MoreMediaTypes;
import no.ssb.dlp.pseudo.service.secrets.PseudoSecrets;
import no.ssb.dlp.pseudo.service.util.Zips;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static no.ssb.dlp.pseudo.service.util.Zips.ZipOptions.zipOpts;

@RequiredArgsConstructor
@Controller
@Slf4j
@Secured(SecurityRule.IS_AUTHENTICATED)
public class ExportController {

    private final DatasetStorage datasetClient;

    private final PseudoSecrets secrets;

    private final GoogleCloudStorageBackend storageBackend;

    @Post("/export")
    public Single<HttpResponse<String>> export(@Body ExportRequest request) {
        PseudoFuncs pseudoFuncs = new PseudoFuncs(request.getPseudoConfig().getRules(), secrets);
        FieldPseudoInterceptor fieldPseudoInterceptor = new FieldPseudoInterceptor(pseudoFuncs, PseudoOperation.DEPSEUDONYMIZE);

        DatasetUri datasetUri = DatasetUri.of(
                request.getDatasetIdentifier().getParentUri().toString(),
                request.getDatasetIdentifier().getPath(),
                request.getDatasetIdentifier().getVersion()
        );
        MediaType targetContentType = MoreMediaTypes.validContentType(request.getTargetContentType());

        // Initiate record stream
        Flowable<Map<String, Object>> records = datasetClient.readParquetRecords(datasetUri, request.getColumnSelectors(), fieldPseudoInterceptor);

        // Serialize records
        Flowable<String> serializedRecords = RecordMapSerializerFactory.newFromMediaType(targetContentType).serialize(records);

        Zips.ZipOptions zipOptions = zipOpts()
                .password(request.getCompression().getPassword())
                .encryptionMethod(request.getCompression().getEncryption())
                .build();

        // Compress stream contents
        Flowable<byte[]> compressedRecords = Zips.zip(serializedRecords, "records.%s".formatted(targetContentType.getExtension().toLowerCase()), zipOptions);

        // Upload stream contents
        URI targetUri = request.getTargetUri();
        return storageBackend
                .write(targetUri.toString(), compressedRecords)
                .timeout(30, TimeUnit.SECONDS)
                .doOnError(throwable -> log.error("Upload failed: %s".formatted(targetUri), throwable))
                .toSingle(() -> {
                    log.info("Successful upload: %s".formatted(targetUri));
                    return HttpResponse.ok();
                });
    }

    @Data
    static class DatasetIdentifier {
        private URI parentUri;
        private String path;
        private String version;
    }

    @Data
    static class ExportRequest {
        private DatasetIdentifier datasetIdentifier;
        private Set<String> columnSelectors;
        private Compression compression;
        private PseudoConfig pseudoConfig;
        private URI targetUri;
        private MediaType targetContentType;
    }
}
