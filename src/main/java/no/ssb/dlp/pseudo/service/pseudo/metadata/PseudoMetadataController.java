package no.ssb.dlp.pseudo.service.pseudo.metadata;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.ssb.dlp.pseudo.core.file.MoreMediaTypes;

import java.util.Collection;

@RequiredArgsConstructor
@Controller
@Slf4j
@Secured(SecurityRule.IS_AUTHENTICATED)
@Tag(name = "Metadata operations")
public class PseudoMetadataController {

    private final PseudoMetadataService pseudoMetadataService;

    @Get("/metadata")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.MULTIPART_FORM_DATA})
    @Produces({MediaType.APPLICATION_JSON, MoreMediaTypes.TEXT_CSV, MoreMediaTypes.APPLICATION_ZIP})
    public HttpResponse<Collection<PseudoMetadata>> listAllMetadata() {
        log.info("List metadata");
        return HttpResponse.ok(pseudoMetadataService.listAll());
    }

}
