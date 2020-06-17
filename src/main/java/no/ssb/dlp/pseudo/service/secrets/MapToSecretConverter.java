package no.ssb.dlp.pseudo.service.secrets;

import io.micronaut.core.convert.ConversionContext;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.convert.TypeConverter;

import javax.inject.Singleton;
import java.util.Map;
import java.util.Optional;

@Singleton
public class MapToSecretConverter implements TypeConverter<Map, Secret> {
    @Override
    public Optional<Secret> convert(Map propertyMap, Class<Secret> targetType, ConversionContext context) {
        Optional<String> content = ConversionService.SHARED.convert(propertyMap.get("content"), String.class);
        Optional<String> type = ConversionService.SHARED.convert(propertyMap.get("type"), String.class);

        return content.isPresent()
          ? Optional.of(new Secret(content.get(), type.orElse(null)))
          : Optional.empty();
    }
}
