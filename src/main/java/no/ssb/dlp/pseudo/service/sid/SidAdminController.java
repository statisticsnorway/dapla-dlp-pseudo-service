package no.ssb.dlp.pseudo.service.sid;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
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

import java.time.Instant;

@RequiredArgsConstructor
@Controller("/sid")
@Slf4j
@Secured(SecurityRule.IS_AUTHENTICATED)
@Tag(name = "Admin operations")
public class SidAdminController {

    private final SidCacheLoader sidCacheLoader;

    @Get("/cache")
    public HttpResponse<SidCacheInfo> getCacheInfo() {
        return HttpResponse.ok(SidCacheInfo.builder()
                .size(sidCacheLoader.getSidCache().size())
                .lastUpdated(sidCacheLoader.getSidCache().getLastUpdated())
                .source(sidCacheLoader.getSource())
                .build()
        );
    }

    @Secured({PseudoServiceRole.ADMIN})
    @ExecuteOn(TaskExecutors.IO)
    @Post("/cache")
    public HttpResponse<String> reloadSidCache() {
        sidCacheLoader.loadSidData(null);
        return HttpResponse.ok();
    }

    @Data
    @Builder
    public static class SidCacheInfo {
        private final int size;

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private final Instant lastUpdated;

        private final String source;
    }

}
