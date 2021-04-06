package no.ssb.dlp.pseudo.service.useraccess;

import io.micronaut.context.annotation.Requires;
import io.reactivex.Single;
import lombok.RequiredArgsConstructor;
import no.ssb.dapla.dataset.api.DatasetState;
import no.ssb.dapla.dataset.api.Valuation;
import no.ssb.dlp.pseudo.service.security.StaticRolesConfig;

import javax.inject.Singleton;

/**
 * UserAccessService that grants access iff the userId is among a preconfigured set of admin users.
 */
@RequiredArgsConstructor
@Singleton
@Requires(property = UserAccessService.CONFIG_PREFIX + ".impl", value="STATIC")
public class StaticUserAccessService implements UserAccessService {

    private final StaticRolesConfig staticRolesConfig;

    @Override
    public Single<Boolean> hasAccess(String userId, DatasetPrivilege privilege, String path, Valuation valuation, DatasetState state) {
        return Single.just(
          staticRolesConfig.getAdmins().stream()
            .anyMatch(admin -> admin.equals(userId))
        );
    }

}
