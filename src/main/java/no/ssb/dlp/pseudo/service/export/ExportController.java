package no.ssb.dlp.pseudo.service.export;


import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.hateoas.JsonError;
import io.micronaut.http.hateoas.Link;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.validation.Validated;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.ssb.dlp.pseudo.core.PseudoFuncRule;
import no.ssb.dlp.pseudo.core.file.Compression;
import no.ssb.dlp.pseudo.core.file.CompressionEncryptionMethod;
import no.ssb.dlp.pseudo.core.file.MoreMediaTypes;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Controller
@Slf4j
@Secured(SecurityRule.IS_AUTHENTICATED)
@Validated
@Tag(name = "Pseudo operations")
public class ExportController {

    private final ExportService exportService;

    @Operation(
            summary = "Export",
            description = """
            Export a dataset in GCS to CSV or JSON, and optionally depseudonymize the data. The dataset will be
            archived in an encrypted zip file protected by a user provided password.

            It is possible to specify `columnSelectors`, that allows for _partial export_, e.g. only specific fields.
            This can be applied as a means to perform data minimization.
            
            Data is exported and stored to a specific, predefined GCS bucket. This is specified in the application
            configuration and cannot be overridden.
            """
    )
    @Post("/export")
    @ExecuteOn(TaskExecutors.IO)
    public ExportService.DatasetExportResult export(@Body @Valid ExportRequest request, Principal principal) {
        log.info("Export dataset - user={}, dataset={}", principal.getName(), request.getSourceDataset());

        ExportService.DatasetExport datasetExport = ExportService.DatasetExport.builder()
          .userId(principal.getName())
          .sourceDataset(request.getSourceDataset())
          .columnSelectors(request.getColumnSelectors() == null ? Set.of() : request.getColumnSelectors())
          .depseudonymize(request.getDepseudonymize())
          .pseudoRules(request.getPseudoRules() == null ? List.of() : request.getPseudoRules())
          .compression(Compression.builder()
            .encryption(CompressionEncryptionMethod.AES)
            .type(MoreMediaTypes.APPLICATION_ZIP_TYPE)
            .password(request.getTargetPassword())
            .build())
          .targetContentName(request.getTargetContentName())
          .targetContentType(request.getTargetContentType())
          .build();

        return exportService.export(datasetExport);
    }

    @Error
    public HttpResponse<JsonError> datasetNotFoundError(HttpRequest<?> request, ExportService.DatasetNotFoundException e) {
        return error(request, e, HttpStatus.BAD_REQUEST, "dataset not found");
    }

    @Error
    public HttpResponse<JsonError> noPseudoRulesFoundError(HttpRequest<?> request, ExportService.NoPseudoRulesFoundException e) {
        return error(request, e, HttpStatus.BAD_REQUEST, "no pseudo rules found for dataset");
    }

    static HttpResponse<JsonError> error(HttpRequest<?> request, Exception e, HttpStatus httpStatus, String httpStatusReason) {
        JsonError error = new JsonError(e.getMessage())
          .link(Link.SELF, Link.of(request.getUri()));

        return HttpResponse.<JsonError>status(httpStatus, httpStatusReason)
          .body(error);
    }

    @Data
    @Introspected
    static class ExportRequest {

        /** Path to dataset to be exported */
        @NotNull
        private ExportService.DatasetUri sourceDataset;

        /**
         * A set of glob patterns that can be used to specify a subset of all fields to export.
         * A dataset can however be exported in its entirety by simply omitting any column selectors.
         */
        private Set<String> columnSelectors = Set.of();

        /**
         * Descriptive name of the contents. This will be used as baseline for the target archive name and its contents.
         * If not specified then this will be deduced from the source dataset name.
         * Should not include file suffixes such as .csv or .json.
         */
        private String targetContentName;

        /**
         * The content type of the resulting file.
         * Defaults to application/json.
         */
        @Schema(implementation = String.class, allowableValues = {
          MediaType.APPLICATION_JSON, MoreMediaTypes.TEXT_CSV})
        private MediaType targetContentType = MediaType.APPLICATION_JSON_TYPE;

        /** The password for the resulting archive */
        @NotBlank
        @Schema(implementation = String.class)
        @Min(9)
        private char[] targetPassword;

        /** Whether or not to depseudonymize dataset during export */
        private Boolean depseudonymize = false;

        /**
         * <p>Pseudonymization rules to be used to depseudonymize the dataset. This is only
         * relevant if depseudonymize=true.</p>
         *
         * <p>If not specified then pseudonymization rules are read from the
         * dataset's dataset-meta.json file.</p>
         */
        private List<PseudoFuncRule> pseudoRules = new ArrayList<>();
    }
}
