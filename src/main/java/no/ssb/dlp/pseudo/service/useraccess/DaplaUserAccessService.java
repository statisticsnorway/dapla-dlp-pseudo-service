package no.ssb.dlp.pseudo.service.useraccess;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.reactivex.Single;
import lombok.RequiredArgsConstructor;
import no.ssb.dapla.dataset.api.DatasetState;
import no.ssb.dapla.dataset.api.Valuation;

import javax.inject.Singleton;

/**
 * UserAccessService that checks access against the dapla user access service. This is the default service that will be
 * used if no other service is configured explicitly.
 */
@RequiredArgsConstructor
@Singleton
@Requires(property = UserAccessService.CONFIG_PREFIX + ".impl", value="DAPLA", defaultValue="DAPLA")
public class DaplaUserAccessService implements UserAccessService {

    private final DaplaUserAccessServiceClient client;

    @Override
    public Single<Boolean> hasAccess(String userId, DatasetPrivilege privilege, String path, Valuation valuation, DatasetState state) {
            return client.hasAccess(userId, privilege, path, valuation, state)
              .onErrorReturn(e -> HttpResponse.status(HttpStatus.FORBIDDEN))
              .map(res -> res.status().getCode() == 200);
    }

}