package no.ssb.dlp.pseudo.service.sid;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.ssb.dlp.pseudo.service.security.PseudoServiceRole;
import org.reactivestreams.Publisher;

import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
@Controller("/sid")
@Slf4j
@Secured(PseudoServiceRole.ADMIN)
@Tag(name = "SID operations")
public class SidLookupController {

    private final SidService sidService;

    @Operation(summary = "Look up missing FNRs", description = "Look FNRs that are not in the SID catalog")
    @Parameter(name = "snapshot", in = ParameterIn.QUERY, description = "Snapshot date of the SID catalog version")
    @Produces(MediaType.APPLICATION_JSON)
    @ExecuteOn(TaskExecutors.IO)
    @Secured({PseudoServiceRole.USER, PseudoServiceRole.ADMIN})
    @Post("/lookup/batch")
    public Publisher<MultiSidLookupResponse> lookupMissing(@QueryValue Optional<String> snapshot, @Body MultiSidRequest req) {
        return sidService.lookupMissing(req.getFnrList(), snapshot);
    }

    @ExecuteOn(TaskExecutors.IO)
    @Post("/map/batch")
    public Publisher<Map<String, SidInfo>> lookupFnrs(@QueryValue Optional<String> snapshot, @Body MultiSidRequest req) {
        return sidService.lookupFnr(req.getFnrList(), snapshot);
    }

    @ExecuteOn(TaskExecutors.IO)
    @Get("/fnr/{fnr}")
    public Publisher<SidInfo> lookupFnr(@PathVariable String fnr, @QueryValue Optional<String> snapshot) {
        return sidService.lookupFnr(fnr, snapshot);
    }

    @ExecuteOn(TaskExecutors.IO)
    @Get("/snr/{snr}")
    public Publisher<SidInfo> lookupSnr(@PathVariable String snr, @QueryValue Optional<String> snapshot) {
        Publisher<SidInfo> sidInfoPublisher = sidService.lookupSnr(snr, snapshot);
        return sidInfoPublisher;
    }


}
