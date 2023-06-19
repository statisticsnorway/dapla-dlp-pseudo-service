package no.ssb.dlp.pseudo.service.pseudo;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import no.ssb.dlp.pseudo.core.field.FieldPseudonymizer;
import no.ssb.dlp.pseudo.core.func.PseudoFuncRule;
import no.ssb.dlp.pseudo.core.tink.model.EncryptedKeysetWrapper;
import no.ssb.dlp.pseudo.core.tink.model.KeysetInfo;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@MicronautTest
class PseudoFieldTest {

    @Test
    void UsesDefaultPseudoConfigWhenNoKeysetIsSupplied() {
        PseudoField pseudoField = new PseudoField("name", List.of("testValue"), null, null);
        assertEquals(List.of(new PseudoFuncRule("name", "**", pseudoField.getDEFAULT_PSEUDO_FUNC())), pseudoField.getPseudoConfig().getRules());
    }


    @Test
    void UsesCustomPseudoConfigWhenKeysetIsSupplied() {
        int keySetPrimaryKey = 12345;

        EncryptedKeysetWrapper encryptedKeysetWrapper = mock(EncryptedKeysetWrapper.class);
        KeysetInfo keysetInfo = mock(KeysetInfo.class);

        when(encryptedKeysetWrapper.getKeysetInfo()).thenReturn(keysetInfo);
        when(keysetInfo.getPrimaryKeyId()).thenReturn(keySetPrimaryKey);

        PseudoField pseudoField = new PseudoField("name", List.of("testValue"), String.format("daead(keyId=%d)", keySetPrimaryKey), encryptedKeysetWrapper);

        assertEquals(String.format("daead(keyId=%d)", keySetPrimaryKey),
                pseudoField.getPseudoConfig().getRules().get(0).getFunc());
    }

    @Test
    void testPseudonymizeThenGetResponseField() {
        String fieldName = "name";
        List<String> fieldValues = List.of("1", "2", "3");
        List<String> expectedEncryptedFieldValues = List.of("encrypted:1", "encrypted:2", "encrypted:3");

        RecordMapProcessorFactory recordProcessorFactory = mock(RecordMapProcessorFactory.class);
        FieldPseudonymizer fieldPseudonymizer = mock(FieldPseudonymizer.class);
        when(recordProcessorFactory.newFieldPseudonymizer(anyList(), any())).thenReturn(fieldPseudonymizer);

        PseudoField pseudoField = spy(new PseudoField("name", fieldValues, null, null));

        // Mock away pseudonymize, already tested in AbstractPseudFieldTest
        when(pseudoField.pseudonymize(fieldValues, recordProcessorFactory)).thenReturn(expectedEncryptedFieldValues);

        ResponsePseudoField responsePseudoField = pseudoField.pseudonymizeThenGetResponseField(recordProcessorFactory);

        //Response should contain the same fieldName
        assertEquals(fieldName, responsePseudoField.getFieldName());
        //Response should contain the same pseudoConfig
        assertEquals(pseudoField.getPseudoConfig(), responsePseudoField.getPseudoRules());
        //Response encrypted values should be prefixed with "encrypted:"
        assertEquals(expectedEncryptedFieldValues, responsePseudoField.getValues());
    }


}