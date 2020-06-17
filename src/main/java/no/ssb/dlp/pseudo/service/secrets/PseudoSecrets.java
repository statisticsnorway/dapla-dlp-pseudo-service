package no.ssb.dlp.pseudo.service.secrets;


import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.convert.format.MapFormat;
import lombok.Data;

import java.util.Map;
import java.util.Optional;

@Data
@ConfigurationProperties("pseudo.secrets")
public class PseudoSecrets {

    @MapFormat(transformation = MapFormat.MapTransformation.NESTED)
    Map<String, Secret> repo;

    public Optional<Secret> getSecret(String secretId) {
        return Optional.ofNullable(repo.get(secretId));
    }

}
