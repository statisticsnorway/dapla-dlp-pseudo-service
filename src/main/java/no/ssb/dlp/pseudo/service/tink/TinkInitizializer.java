package no.ssb.dlp.pseudo.service.tink;

import com.google.crypto.tink.daead.DeterministicAeadConfig;
import com.google.crypto.tink.integration.gcpkms.GcpKmsClient;
import io.micronaut.context.annotation.Context;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.util.Optional;

@Context
@Singleton
@Slf4j
public class TinkInitizializer {

    public TinkInitizializer(KmsConfig kmsConfig) {
        try {
            DeterministicAeadConfig.register();
            GcpKmsClient.register(Optional.of(kmsConfig.getMasterKek()), Optional.empty());
            log.info("Initialized Tink GcpKmsClient");
        }
        catch (Exception e) {
            throw new RuntimeException("Error initializing tink", e);
        }

    }
}
