package no.ssb.dlp.pseudo.service.pseudo;

import io.micronaut.core.convert.MutableConversionService;
import io.micronaut.core.convert.TypeConverterRegistrar;
import jakarta.inject.Singleton;
import no.ssb.dlp.pseudo.core.PseudoSecret;
import no.ssb.dlp.pseudo.core.func.PseudoFuncRule;
import no.ssb.dlp.pseudo.core.typeconverter.PseudoFuncRuleTypeConverter;
import no.ssb.dlp.pseudo.core.typeconverter.PseudoSecretTypeConverter;

import java.util.Map;

/**
 *  Micronaut {@link io.micronaut.core.convert.TypeConverter} implementations defined in a different
 *  package like the ones from {@link no.ssb.dlp.pseudo.core} need to be manually registered
 *  using this {@link io.micronaut.core.convert.TypeConverterRegistrar} implementation.
 *
 * @author Nicholas Jaunsen
 */
@Singleton
public class PseudoServiceTypeConverterRegistrar implements TypeConverterRegistrar {
    @Override
    public void register(MutableConversionService conversionService) {
        conversionService.addConverter(Map.class, PseudoSecret.class, new PseudoSecretTypeConverter());
        conversionService.addConverter(Map.class, PseudoFuncRule.class, new PseudoFuncRuleTypeConverter());
    }
}