package no.ssb.dlp.pseudo.service.useraccess;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.client.annotation.Client;
import io.reactivex.Single;
import no.ssb.dapla.dataset.api.DatasetState;
import no.ssb.dapla.dataset.api.Valuation;

@Requires(property = UserAccessService.CONFIG_PREFIX + ".impl", value="HTTP", defaultValue="HTTP")
@Client(UserAccessService.SERVICE_ID)
public interface DaplaUserAccessServiceClient {

    @Get("/access/{userId}")
    Single<HttpResponse<Void>> hasAccess(@PathVariable String userId, @QueryValue UserAccessService.DatasetPrivilege privilege, @QueryValue String path, @QueryValue Valuation valuation, @QueryValue DatasetState state);

}