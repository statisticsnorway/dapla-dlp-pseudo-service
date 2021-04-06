package no.ssb.dlp.pseudo.service.export;


import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.validation.Validated;
import io.reactivex.Single;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.ssb.dapla.dataset.api.DatasetId;
import no.ssb.dlp.pseudo.core.file.Compression;
import no.ssb.dlp.pseudo.core.file.CompressionEncryptionMethod;
import no.ssb.dlp.pseudo.core.file.MoreMediaTypes;
import no.ssb.dlp.pseudo.core.util.PathJoiner;
import no.ssb.dlp.pseudo.service.pseudo.PseudoConfig;
import no.ssb.dlp.pseudo.service.security.PseudoServiceRole;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.security.Principal;
import java.util.Set;

@RequiredArgsConstructor
@Controller
@Slf4j
@Secured(SecurityRule.IS_AUTHENTICATED)
@Validated
public class ExportController {

    private final ExportConfig exportConfig;
    private final ExportService exportService;

    @Post("/export")
    @Secured({PseudoServiceRole.ADMIN})
    public Single<ExportService.DatasetExportResult> export(@Body @Valid ExportRequest request, Principal principal) {
        log.info("Export dataset - user={}, dataset={}", principal.getName(), request.getSourceDataset().getPath());

        ExportService.DatasetExport datasetExport = ExportService.DatasetExport.builder()
          .userId(principal.getName())
          .sourceDatasetId(request.getSourceDataset().datasetId())
          .columnSelectors(request.getColumnSelectors())
          .depseudonymize(request.getDepseudonymize())
          .pseudoConfig(request.getPseudoConfig())
          .compression(Compression.builder()
            .encryption(CompressionEncryptionMethod.AES)
            .type(MoreMediaTypes.APPLICATION_ZIP_TYPE)
            .password(request.getTarget().getPassword())
            .build())
          .targetPath(PathJoiner.joinWithoutLeadingOrTrailingSlash(exportConfig.getDefaultTargetRoot(), request.getTarget().getPath()))
          .targetName(request.getTarget().getContentName())
          .targetContentType(request.getTarget().getContentType())
          .build();

        return exportService.export(datasetExport);
    }

   @Data
    static class DatasetIdentifier {
        private String path;
        private String timestamp;

       DatasetId datasetId() {
            return DatasetId.newBuilder()
              .setPath(path)
              .setVersion(timestamp)
              .build();
        }
    }

    @Data
    @Introspected
    static class ExportRequest {

        @NotNull
        private DatasetIdentifier sourceDataset;

        /**
         * A set of glob patterns that can be used to specify a subset of all fields to export.
         * A dataset can however be exported in its entirety by simply omitting any column selectors.
         */
        private Set<String> columnSelectors = Set.of();

        /** Whether or not to depseudonymize dataset during export */
        private Boolean depseudonymize;

        /**
         * The pseudonymization config to apply - if depseudonymize = true
         */
        private PseudoConfig pseudoConfig = new PseudoConfig();

        @NotNull
        private ExportTarget target;
    }

    @Introspected
    @Data
    static class ExportTarget {
        private String path;

        /**
         * Descriptive name of the contents. This will be used as baseline for the target archive name and its contents.
         */
        @NotNull
        private String contentName;

        /** The content type of the resulting file. */
        @Schema(implementation = String.class, allowableValues = {
          MediaType.APPLICATION_JSON, MoreMediaTypes.TEXT_CSV})
        @NotNull
        private MediaType contentType;

        /** The password on the resulting archive */
        @NotBlank
        @Schema(implementation = String.class)
        @Min(9)
        private char[] password;
    }

}
