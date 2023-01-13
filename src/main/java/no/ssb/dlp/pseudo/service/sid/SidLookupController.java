package no.ssb.dlp.pseudo.service.sid;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.hateoas.JsonError;
import io.micronaut.http.hateoas.Link;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.ssb.dlp.pseudo.service.security.PseudoServiceRole;

@RequiredArgsConstructor
@Controller("/sid")
@Slf4j
@Secured(SecurityRule.IS_AUTHENTICATED)
@Tag(name = "SID operations")
public class SidLookupController {
    private final SidCache sidCache;

    @Secured({PseudoServiceRole.ADMIN})
    @ExecuteOn(TaskExecutors.IO)
    @Get("/fnr/{fnr}")
    public HttpResponse<SidInfo> lookupFnr(@PathVariable String fnr) {
        SidItem sidItem = sidCache.getSidItemForFnr(fnr)
                .orElseThrow(() -> new NoSidMappingFoundException("No SID matching fnr=" + fnr));

        return HttpResponse.ok(SidInfo.from(sidItem));
    }

    @Secured({PseudoServiceRole.ADMIN})
    @ExecuteOn(TaskExecutors.IO)
    @Get("/snr/{snr}")
    public HttpResponse<SidInfo> lookupSnr(@PathVariable String snr) {
        SidItem sidItem = sidCache.getSidItemForSnr(snr)
                .orElseThrow(() -> new NoSidMappingFoundException("No SID matching snr=" + snr));

        return HttpResponse.ok(SidInfo.from(sidItem));
    }

    @Data
    @Builder
    public static class SidInfo {
        private final String currentSnr;
        private final String currentFnr;

        public static SidInfo from(SidItem sidItem) {
            return SidInfo.builder()
                    .currentSnr(sidItem.getCurrentSnr())
                    .currentFnr(sidItem.getFnrCurrent())
                    .build();
        }
    }

    @Error
    public HttpResponse<JsonError> noSidMappingFoundError(HttpRequest request, NoSidMappingFoundException e) {
        JsonError error = new JsonError(e.getMessage())
                .link(Link.SELF, Link.of(request.getUri()));
        return HttpResponse.<JsonError>notFound().body(error);
    }

    public static class NoSidMappingFoundException extends RuntimeException {
        public NoSidMappingFoundException(String message) {
            super(message);
        }
    }

}
