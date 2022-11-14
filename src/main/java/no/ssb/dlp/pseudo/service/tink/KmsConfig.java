package no.ssb.dlp.pseudo.service.tink;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

@ConfigurationProperties("gcp.kms")
@Data
public class KmsConfig {
    private String masterKek;
}
