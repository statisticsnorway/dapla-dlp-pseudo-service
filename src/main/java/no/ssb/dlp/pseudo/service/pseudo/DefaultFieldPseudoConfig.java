package no.ssb.dlp.pseudo.service.pseudo;

import lombok.Getter;
import no.ssb.dlp.pseudo.core.func.PseudoFuncRule;

import javax.inject.Singleton;
/**
 * Default PseudoConfig used when pseudonymizing fields.
 * This class reduces the overhead associated with recreating the PseudoConfig by providing pre-configured default values.
 */
@Singleton
@Getter
public class DefaultFieldPseudoConfig {

    private final PseudoConfig defaultPseudoConfig = new PseudoConfig(new PseudoFuncRule("default", "**",
            "daead(keyId=ssb-common-key-1)"));

    private final PseudoConfig defualtSIDPseudoConfig = new PseudoConfig(new PseudoFuncRule("default-sid", "**",
            "ff31(keyId=papis-key-1, strategy=SKIP)"));
}
