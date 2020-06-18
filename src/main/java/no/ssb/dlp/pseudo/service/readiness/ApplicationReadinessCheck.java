package no.ssb.dlp.pseudo.service.readiness;

import no.ssb.dapla.readiness.ReadinessCheck;

import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

@Singleton
public class ApplicationReadinessCheck implements ReadinessCheck {

    @Override
    public CompletableFuture<Boolean> check() {
        return CompletableFuture.supplyAsync(() -> true);
    }
}
