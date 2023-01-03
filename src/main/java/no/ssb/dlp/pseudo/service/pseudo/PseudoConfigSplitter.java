package no.ssb.dlp.pseudo.service.pseudo;

import no.ssb.dlp.pseudo.core.PseudoFuncRule;

import javax.inject.Singleton;
import java.util.List;

/**
 * Splits a PseudoConfig into mulitple PseudoConfig objects to support chaining of SID Mapping functions
 *
 * Disclaimer: This is a stop gap "solution", used to support chaining of SID Mapping with pseudonymization.
 * The API contract could be changed to support multiple PseudoConfig transformations, but we need to think more aobut
 * exactly how this should be expressed.
 */
@Singleton
public class PseudoConfigSplitter {

    public List<PseudoConfig> splitIfNecessary(PseudoConfig pseudoConfig) {
        PseudoConfig configWithOnlyMappingRules = filterMappingRules(pseudoConfig);

        if (configWithOnlyMappingRules.getRules().isEmpty()) {
            return List.of(pseudoConfig);
        }
        else {
            PseudoConfig replacedSidMappingRules = replaceSidMappingRules(pseudoConfig, "tink-daead");
            return List.of(configWithOnlyMappingRules, replacedSidMappingRules);
        }
    }

    private static PseudoConfig filterMappingRules(PseudoConfig pseudoConfig) {
        PseudoConfig filteredPseudoConfig = new PseudoConfig();
        filteredPseudoConfig.setRules(pseudoConfig.getRules().stream()
                .filter(r -> r.getFunc().startsWith("map-sid"))
                .toList()
        );

        return filteredPseudoConfig;
    }

    private static PseudoConfig replaceSidMappingRules(PseudoConfig pseudoConfig, String newName) {
        pseudoConfig.setRules(pseudoConfig.getRules().stream()
                .map(r -> {
                    if (r.getFunc().startsWith("map-sid")) {
                        String newFuncName = newName + r.getFunc().substring(7);
                        return new PseudoFuncRule(r.getName(), r.getPattern(), newFuncName);
                    }
                    else {
                        return r;
                    }
                })
                .toList()
        );

        return pseudoConfig;
    }


}
