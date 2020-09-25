package no.ssb.dlp.pseudo.service.storage;

import io.micronaut.context.annotation.Factory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.ssb.dapla.storage.client.DatasetStorage;
import no.ssb.dapla.storage.client.backend.gcs.GoogleCloudStorageBackend;

import javax.inject.Singleton;

@Factory
@RequiredArgsConstructor
@Slf4j
public class DatasetStorageFactory {

    private final GoogleCloudStorageBackend googleCloudStorageBackend;

    @Singleton
    public DatasetStorage datasetStorage() {
        return DatasetStorage.builder().withBinaryBackend(googleCloudStorageBackend).build();
    }
}
