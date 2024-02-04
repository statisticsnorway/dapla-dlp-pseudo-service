package no.ssb.dlp.pseudo.service.pseudo.metadata;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Produces;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import no.ssb.dapla.metadata.PseudonymizationMetadata;
import no.ssb.dlp.pseudo.core.file.MoreMediaTypes;
import no.ssb.dlp.pseudo.service.security.PseudoServiceRole;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Controller
@Slf4j
@Secured({PseudoServiceRole.USER, PseudoServiceRole.ADMIN})
@Tag(name = "Metadata operations")
public class PseudoMetadataController {

    private final PseudoMetadataService pseudoMetadataService;

    @Operation(
            summary = "List all metadata"
    )
    @Get("/metadata")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.MULTIPART_FORM_DATA})
    @Produces({MediaType.APPLICATION_JSON, MoreMediaTypes.TEXT_CSV, MoreMediaTypes.APPLICATION_ZIP})
    public HttpResponse<Collection<PseudoMetadataResponse>> listAllMetadata() {
        log.info("List metadata");
        return HttpResponse.ok(pseudoMetadataService.listAll().stream().map(this::toPseudoMetadataResponse).toList());
    }

    @Get("/metadata/{correlationId}")
    public HttpResponse<PseudoMetadataResponse> findById(@PathVariable String correlationId) {
        log.info("Get metadata for correlationId={}", correlationId);
        return pseudoMetadataService.findById(correlationId).map(this::toPseudoMetadataResponse).map(HttpResponse::ok)
                .orElseGet(HttpResponse::notFound);
    }

    private PseudoMetadataResponse toPseudoMetadataResponse(PseudoMetadata pseudoMetadata) {
        return PseudoMetadataResponse.builder()
                .pseudonymizationMetadata(pseudoMetadata.toPseudonymizationMetadata())
                .warnings(pseudoMetadata.getWarnings())
                .build();
    }
    @Value
    @Builder
    public static class PseudoMetadataResponse {
        private final PseudonymizationMetadata pseudonymizationMetadata;
        private final Map<String, List<String>> warnings;
    }
}
