package no.ssb.dlp.pseudo.service.secrets;


import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@ConfigurationProperties(SecretServiceConfig.PREFIX)
public class SecretServiceConfig {

    public static final String PREFIX = "services.secrets";

    public enum Impl {
        GCP, LOCAL, MOCK;
    }

    /**
     * The SecretService implementation to use. Defaults to MOCK if not specified.
     */
    private Impl impl = Impl.MOCK;

    /**
     * A map of hardcoded secrets that will override any secret IDs. This can be handy in testing situations. Secrets
     * that are defined in this map will not be looked up in SecretManager (if impl=GCP).
     */
    Map<String, byte[]> overrides = new LinkedHashMap<>();

}
