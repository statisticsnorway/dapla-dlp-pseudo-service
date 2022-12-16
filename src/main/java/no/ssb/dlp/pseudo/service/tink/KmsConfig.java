package no.ssb.dlp.pseudo.service.tink;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Context;
import io.micronaut.core.annotation.Introspected;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties("gcp.kms")
@Data
@Context
public class KmsConfig {

    /**
     * URIs to registered KMS KEKs (Key Encryption Keys)
     */
    @NotEmpty
    private List<URI> keyUris = new ArrayList<>();

    /**
     * Optional path to GCP credentials file (Service Account json). If not specified, the application will revert to
     * using end user credentials (which is okay when running in Kubernetes).
     */
    private String credentialsPath;

}
