package no.ssb.dlp.pseudo.service.security;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.security.token.Claims;
import io.micronaut.security.token.DefaultRolesFinder;
import io.micronaut.security.token.RolesFinder;
import io.micronaut.security.token.config.TokenConfiguration;
import lombok.RequiredArgsConstructor;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
@Replaces(bean = DefaultRolesFinder.class)
@RequiredArgsConstructor
public class CustomRolesFinder implements RolesFinder {

    private final TokenConfiguration tokenConfiguration;
    private final StaticRolesConfig rolesConfig;

    @NonNull
    @Override
    public List<String> findInClaims(@NonNull Claims claims) {
        List<String> roles = new ArrayList<>();

        Object username = claims.get(tokenConfiguration.getNameKey());
        if (rolesConfig.getAdmins().contains(username)) {
            roles.add(PseudoServiceRole.ADMIN);
        }

        return roles;
    }
}
