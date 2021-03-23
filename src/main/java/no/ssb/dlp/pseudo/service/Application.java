package no.ssb.dlp.pseudo.service;

import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.servers.Servers;

@OpenAPIDefinition
@SecurityScheme(name = "BearerAuth",
  type = SecuritySchemeType.HTTP,
  scheme = "bearer",
  bearerFormat = "jwt")
@SecurityRequirement(name = "BearerAuth")
@Servers({
  @Server(url = "http://localhost:30950", description = "Locally proxied pseudo service")
})
public class Application {

    public static void main(String[] args) {
        Micronaut.run(Application.class);
    }
}
