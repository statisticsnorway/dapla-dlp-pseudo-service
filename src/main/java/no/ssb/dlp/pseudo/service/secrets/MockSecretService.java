package no.ssb.dlp.pseudo.service.secrets;

import io.micronaut.context.annotation.Requires;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.nio.charset.StandardCharsets;

@Singleton
@Requires(property = SecretServiceConfig.PREFIX + ".impl", value = "MOCK", defaultValue="MOCK")
@Slf4j
public class MockSecretService implements SecretService {

    static final byte[] MOCKED_VALUE = "mockedsecret".getBytes(StandardCharsets.UTF_8);

    public MockSecretService() {
        log.info("Using MockSecretService");
    }

    @Override
    public byte[] getSecret(String secretId) {
        return MOCKED_VALUE;
    }

    @Override
    public byte[] getSecret(String secretId, String version) {
        return MOCKED_VALUE;
    }

    @Override
    public byte[] getCacheableSecret(String secretId) {
        return MOCKED_VALUE;
    }

    @Override
    public byte[] getCacheableSecret(String secretId, String version) {
        return MOCKED_VALUE;
    }

}
