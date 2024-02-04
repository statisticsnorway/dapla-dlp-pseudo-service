package no.ssb.dlp.pseudo.service.pseudo;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import no.ssb.dlp.pseudo.core.tink.model.EncryptedKeysetWrapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@MicronautTest
class PseudoFieldTest {

    @Test
    void UsesDefaultPseudoConfigWhenNoKeysetIsSupplied() {
        PseudoField pseudoField = new PseudoField(null, null, null);
        assertEquals(PseudoField.getDEFAULT_PSEUDO_FUNC(), pseudoField.getPseudoConfig().getRules().get(0).getFunc());
    }

    @Test
    void usesCustomPseudoFuncWhenPseudoFuncIsSupplied() {
        int keySetPrimaryKey = 12345;

        PseudoField pseudoSIDField = new PseudoField(null,  String.format("map-sid(keyId=%s)", keySetPrimaryKey), null);

        assertEquals(String.format("map-sid(keyId=%s)", keySetPrimaryKey),
                pseudoSIDField.getPseudoConfig().getRules().get(0).getFunc());
    }

    @Test
    void setCustomKeysetWhenKeysetIsSupplied() {
        EncryptedKeysetWrapper encryptedKeysetWrapper = mock(EncryptedKeysetWrapper.class);
        PseudoField pseudoField = new PseudoField(null, null, encryptedKeysetWrapper);

        assertEquals(encryptedKeysetWrapper,
                pseudoField.getPseudoConfig().getKeysets().get(0));
    }
}