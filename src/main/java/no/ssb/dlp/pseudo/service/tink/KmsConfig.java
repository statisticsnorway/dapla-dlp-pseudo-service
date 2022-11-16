package no.ssb.dlp.pseudo.service.tink;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ConfigurationProperties("gcp.kms")
@Data
public class KmsConfig {

    /**
     * URIs to registered KMS KEKs (Key Encryption Keys)
     */
    private List<URI> keyUris = new ArrayList<>();

    /**
     * Optional path to GCP credentials file (Service Account json). If not specified, the application will revert to
     * using end user credentials (which is okay when running in Kubernetes).
     */
    private String credentialsPath;

}
