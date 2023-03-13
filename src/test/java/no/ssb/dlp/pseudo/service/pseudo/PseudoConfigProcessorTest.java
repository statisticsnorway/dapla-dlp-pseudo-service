package no.ssb.dlp.pseudo.service.pseudo;

import no.ssb.dlp.pseudo.core.func.PseudoFuncRule;
import no.ssb.dlp.pseudo.core.util.Json;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PseudoConfigProcessorTest {

    private PseudoConfigSplitter pseudoConfigProcessor = new PseudoConfigSplitter();

    @Test
    void givenNonSplittableRules_split_shouldNotCreateMultipleConfigs() {
        PseudoConfig pseudoConfig = Json.toObject(PseudoConfig.class, """
                {
                  "rules": [
                    {
                      "name": "navn",
                      "pattern": "**/navn",
                      "func": "daead(keyId=123456789)"
                    },
                    {
                      "name": "fnr",
                      "pattern": "**/fnr",
                      "func": "daead(keyId=123456789)"
                    }
                  ]
                }
                """);
        assertThat(pseudoConfigProcessor.splitIfNecessary(pseudoConfig)).hasSize(1);
    }

    @Test
    void givenSidMappingRule_split_shouldResultInMultipleConfigs() {
        PseudoConfig pseudoConfig = Json.toObject(PseudoConfig.class, """
                {
                  "rules": [
                    {
                      "name": "fnr",
                      "pattern": "**/fnr",
                      "func": "map-sid(keyId=123456789)"
                    }
                  ]
                }
                """);
        List<PseudoConfig> configs = pseudoConfigProcessor.splitIfNecessary(pseudoConfig);
        assertThat(configs).hasSize(2);
        assertThat(configs.get(0).getRules().get(0)).isEqualTo(new PseudoFuncRule("fnr", "**/fnr", "map-sid(keyId=123456789)"));
        assertThat(configs.get(1).getRules().get(0)).isEqualTo(new PseudoFuncRule("fnr", "**/fnr", "ff31(keyId=123456789, strategy=SKIP)"));
    }

    @Test
    void givenMultipleMixedRules_split_shouldResultInMultipleConfigs() {
        PseudoConfig pseudoConfig = Json.toObject(PseudoConfig.class, """
                {
                  "rules": [
                    {
                      "name": "navn",
                      "pattern": "**/navn",
                      "func": "daead(keyId=123456789)"
                    },
                    {
                      "name": "fnr",
                      "pattern": "**/fnr",
                      "func": "map-sid(keyId=ssb-common-key-1)"
                    },
                    {
                      "name": "spouse",
                      "pattern": "**/spouse_id",
                      "func": "map-sid(keyId=ssb-common-key-2)"
                    },
                    {
                      "name": "foo",
                      "pattern": "**/foo",
                      "func": "daead(keyId=123456789)"
                    }
                  ]
                }
                """);
        List<PseudoConfig> configs = pseudoConfigProcessor.splitIfNecessary(pseudoConfig);
        assertThat(configs).hasSize(2);
        PseudoConfig sidMappingPseudoConfig = configs.get(0);
        PseudoConfig encryptionPseudoConfig = configs.get(1);
        assertThat(sidMappingPseudoConfig.getRules()).hasSize(2);
        assertThat(sidMappingPseudoConfig.getRules().get(0)).isEqualTo(new PseudoFuncRule("fnr", "**/fnr", "map-sid(keyId=ssb-common-key-1)"));
        assertThat(sidMappingPseudoConfig.getRules().get(1)).isEqualTo(new PseudoFuncRule("spouse", "**/spouse_id", "map-sid(keyId=ssb-common-key-2)"));
        assertThat(encryptionPseudoConfig.getRules()).hasSize(4);
        assertThat(encryptionPseudoConfig.getRules().get(0)).isEqualTo(new PseudoFuncRule("navn", "**/navn", "daead(keyId=123456789)"));
        assertThat(encryptionPseudoConfig.getRules().get(1)).isEqualTo(new PseudoFuncRule("fnr", "**/fnr", "ff31(keyId=ssb-common-key-1, strategy=SKIP)"));
        assertThat(encryptionPseudoConfig.getRules().get(2)).isEqualTo(new PseudoFuncRule("spouse", "**/spouse_id", "ff31(keyId=ssb-common-key-2, strategy=SKIP)"));
        assertThat(encryptionPseudoConfig.getRules().get(3)).isEqualTo(new PseudoFuncRule("foo", "**/foo", "daead(keyId=123456789)"));
    }


}