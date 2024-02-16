package no.ssb.dlp.pseudo.service.security;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ConfigurationProperties("app-roles")
@Data
public class StaticRolesConfig {
    @NotBlank
    private List<String> users = new ArrayList<>();
    private List<String> admins = new ArrayList<>();

    private Optional<String> usersGroup = Optional.empty();
    private Optional<String> adminsGroup = Optional.empty();
}
