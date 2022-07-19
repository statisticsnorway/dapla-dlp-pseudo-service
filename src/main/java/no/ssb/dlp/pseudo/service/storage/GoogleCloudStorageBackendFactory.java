package no.ssb.dlp.pseudo.service.storage;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.ssb.dapla.storage.client.backend.gcs.GoogleCloudStorageBackend;
import no.ssb.dapla.storage.client.backend.gcs.GoogleCloudStorageBackend.Configuration;

import javax.annotation.Nullable;
import javax.inject.Singleton;
import java.nio.file.Files;
import java.nio.file.Path;

@Factory
@RequiredArgsConstructor
@Slf4j
public class GoogleCloudStorageBackendFactory {

    @Nullable
    @Property(name = "storage.gcs-service-account-file")
    private Path serviceAccountCredentials;

    @Singleton
    public GoogleCloudStorageBackend googleCloudStorageBackend() {
        Configuration configuration = new Configuration();
        if (serviceAccountCredentials != null) {
            if (Files.notExists(serviceAccountCredentials)) {
                throw new StorageBackendException("Could not find service account credentials: " + serviceAccountCredentials);
            }
            configuration.setServiceAccountCredentials(serviceAccountCredentials);
        }
        return new GoogleCloudStorageBackend(configuration);
    }

    public static class StorageBackendException extends RuntimeException {
        public StorageBackendException(String message) {
            super(message);
        }
    }

}
