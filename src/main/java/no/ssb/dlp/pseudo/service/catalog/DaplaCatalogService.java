package no.ssb.dlp.pseudo.service.catalog;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;
import io.reactivex.Single;

@Requires(property = CatalogService.CONFIG_PREFIX + ".impl", value="DAPLA", defaultValue="DAPLA")
@Client(CatalogService.SERVICE_ID)
public interface DaplaCatalogService extends CatalogService {

    @Override
    @Post("/rpc/CatalogService/get")
    Single<CatalogService.GetDatasetResponse> getDataset(@Body CatalogService.GetDatasetRequest request);

}
