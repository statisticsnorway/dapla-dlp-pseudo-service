package no.ssb.dlp.pseudo.service.catalog;

import io.micronaut.context.annotation.Requires;
import io.reactivex.Single;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;

@Slf4j
@Singleton
@Requires(property = CatalogService.CONFIG_PREFIX + ".impl", value="MOCK")
public class MockCatalogService implements CatalogService {

    @Override
    public Single<GetDatasetResponse> getDataset(GetDatasetRequest request) {
        log.info("CatalogService MOCK - getDataset");

        String responseJson = """
        {
          "dataset": {
            "id": {
              "path": "/produkt/foo/bar/blah",
              "timestamp": "1610100133742"
            },
            "valuation": "INTERNAL",
            "state": "INPUT",
            "parentUri": "gs://ssb-data-dev-produkt",
            "pseudoConfig": {}
          }
        }
        """;

        GetDatasetResponse res = no.ssb.dlp.pseudo.core.util.Json.toObject(GetDatasetResponse.class, responseJson);
        return Single.just(res);
    }
}
