package no.ssb.dlp.pseudo.service.useraccess;

import io.micronaut.context.annotation.Requires;
import io.reactivex.Single;
import lombok.extern.slf4j.Slf4j;
import no.ssb.dapla.dataset.api.DatasetState;
import no.ssb.dapla.dataset.api.Valuation;

import javax.inject.Singleton;

/**
 * UserAccessService that always grants access - can be used in testing scenarios where you want to skip user access
 * validation.
 */
@Slf4j
@Singleton
@Requires(property = UserAccessService.CONFIG_PREFIX + ".impl", value="MOCK")
public class MockUserAccessService implements UserAccessService {

    @Override
    public Single<Boolean> hasAccess(String userId, DatasetPrivilege privilege, String path, Valuation valuation, DatasetState state) {
        log.info("UserAccessService MOCK - hasAccess");
        return Single.just(Boolean.TRUE);
    }

}
