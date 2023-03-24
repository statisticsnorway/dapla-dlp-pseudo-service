package no.ssb.dlp.pseudo.service.sid;

import io.micronaut.context.annotation.Requirements;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Post;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.ssb.dlp.pseudo.service.security.PseudoServiceRole;
import org.reactivestreams.Publisher;

@RequiredArgsConstructor
@Controller("/local-sid")
@Slf4j
@Secured(SecurityRule.IS_AUTHENTICATED)
@Tag(name = "SID operations")
@Requirements({
        @Requires(
                env = {"local-sid"}
        )
})
public class LocalSidServiceController {
    private final LocalSidService sidService = new LocalSidService();

    @Secured({PseudoServiceRole.ADMIN})
    @ExecuteOn(TaskExecutors.IO)
    @Post("/sid/map")
    public Publisher<SidInfo> lookup(@Body SidRequest sidRequest, @Header("Authorization") String auth) {
        if (sidRequest.getFnr() != null) {
            return sidService.lookupFnr(sidRequest.getFnr());
        }
        if (sidRequest.getSnr() != null) {
            return sidService.lookupSnr(sidRequest.getSnr());
        }
        return null;
    }

}
