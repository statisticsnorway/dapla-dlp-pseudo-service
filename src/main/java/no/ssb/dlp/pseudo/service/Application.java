package no.ssb.dlp.pseudo.service;

import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.servers.Servers;
import lombok.extern.slf4j.Slf4j;

@OpenAPIDefinition
@SecurityScheme(name = "BearerAuth",
  type = SecuritySchemeType.HTTP,
  scheme = "bearer",
  bearerFormat = "jwt")
@SecurityRequirement(name = "BearerAuth")
@Server(url = "http://localhost:10210", description = "Locally proxied pseudo service")
@Slf4j
public class Application {

    public static void main(String[] args) {
        log.info("pseudo-service version: {}", BuildInfo.INSTANCE.getVersionAndBuildTimestamp());
        Micronaut.run(Application.class);
    }
}
