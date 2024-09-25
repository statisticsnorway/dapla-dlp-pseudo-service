package no.ssb.dlp.pseudo.service.sid;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
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

import java.util.Optional;

@RequiredArgsConstructor
@Controller("/local-sid")
@Slf4j
@Secured(SecurityRule.IS_AUTHENTICATED)
@Tag(name = "Look up SIDs locally. This controller is only enabled for local-sid environment")
@Requires(env = "local-sid")
public class LocalSidServiceController {
    private final SidService sidService;

    @Secured({PseudoServiceRole.ADMIN})
    @ExecuteOn(TaskExecutors.BLOCKING)
    @Post("/sid/map")
    public Publisher<SidInfo> lookup(@Body SidRequest sidRequest) {
        if (sidRequest.fnr() != null) {
            return sidService.lookupFnr(sidRequest.fnr(), Optional.empty());
        }
        if (sidRequest.snr() != null) {
            return sidService.lookupSnr(sidRequest.snr(), Optional.empty());
        }
        return null;
    }

}
