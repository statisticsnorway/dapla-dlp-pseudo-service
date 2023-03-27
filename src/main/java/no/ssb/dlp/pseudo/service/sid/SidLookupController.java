package no.ssb.dlp.pseudo.service.sid;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.ssb.dlp.pseudo.service.security.PseudoServiceRole;
import org.reactivestreams.Publisher;

@RequiredArgsConstructor
@Controller("/sid")
@Slf4j
@Secured(SecurityRule.IS_AUTHENTICATED)
@Tag(name = "SID operations")
public class SidLookupController {
    @NonNull
    private final SidClient sidClient;

    @Secured({PseudoServiceRole.ADMIN})
    @ExecuteOn(TaskExecutors.IO)
    @Get("/fnr/{fnr}")
    public Publisher<SidInfo> lookupFnr(@PathVariable String fnr) {
        return sidClient.lookup(new SidRequest.SidRequestBuilder().fnr(fnr).build());
    }

    @Secured({PseudoServiceRole.ADMIN})
    @ExecuteOn(TaskExecutors.IO)
    @Get("/snr/{snr}")
    public Publisher<SidInfo> lookupSnr(@PathVariable String snr) {
        return sidClient.lookup(new SidRequest.SidRequestBuilder().snr(snr).build());
    }


}
