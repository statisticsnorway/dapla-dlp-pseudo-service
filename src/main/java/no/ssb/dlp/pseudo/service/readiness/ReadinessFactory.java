package no.ssb.dlp.pseudo.service.readiness;

import io.micronaut.context.annotation.Factory;
import lombok.extern.slf4j.Slf4j;
import no.ssb.dapla.readiness.Readiness;

import javax.inject.Singleton;

@Slf4j
@Factory
public class ReadinessFactory {
    @Singleton
    public Readiness readiness(ApplicationReadinessCheck readinessCheck) {
        return Readiness.newBuilder(readinessCheck)
                .setMinSampleInterval(60000) //Limit time between samples to 60 seconds
                .setBlockingReadinessCheckMaxAttempts(2)
                .build();
    }
}
