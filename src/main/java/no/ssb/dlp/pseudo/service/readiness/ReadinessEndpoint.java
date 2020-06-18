package no.ssb.dlp.pseudo.service.readiness;

import io.micronaut.management.endpoint.annotation.Read;
import io.micronaut.management.endpoint.annotation.Endpoint;
import lombok.extern.slf4j.Slf4j;
import no.ssb.dapla.readiness.Readiness;
import no.ssb.dapla.readiness.ReadinessSample;

@Slf4j
@Endpoint(id = "ready", defaultSensitive = false)
public class ReadinessEndpoint {

    private final Readiness readiness;

    public ReadinessEndpoint(Readiness readiness) {
        this.readiness = readiness;
    }

    /**
     * Check readiness and return '200 OK' if ready, '404 Not Found' if not.
     */
    @Read
    public String isReady() {
        ReadinessSample readinessSample = readiness.getAndKeepaliveReadinessSample();
        return readinessSample.isReady() ? "READY" : null;
    }
}
