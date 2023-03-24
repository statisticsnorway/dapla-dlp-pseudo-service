package no.ssb.dlp.pseudo.service.sid;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

@Data
@ConfigurationProperties(SidServiceConfig.PREFIX)
public class SidServiceConfig {

    public static final String PREFIX = "services.sid";

    public enum Impl {
        GCP, LOCAL;
    }
    /**
     * The SidService implementation to use. Defaults to LOCAL if not specified.
     */
    private Impl impl = Impl.LOCAL;
}
