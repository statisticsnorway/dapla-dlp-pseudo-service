package no.ssb.dlp.pseudo.service.pseudo;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import no.ssb.dlp.pseudo.core.field.FieldPseudonymizer;
import no.ssb.dlp.pseudo.core.tink.model.EncryptedKeysetWrapper;
import no.ssb.dlp.pseudo.core.tink.model.KeysetInfo;
import no.ssb.dlp.pseudo.service.sid.SidMapper;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@MicronautTest
class PseudoSIDFieldTest {
    @Test
    void usesDefaultPseudoConfigWhenNoKeysetIsSupplied() {
        PseudoSIDField pseudoSIDField = new PseudoSIDField("name", List.of("testValue"), null);
        assertEquals(pseudoSIDField.getDEFAULT_SID_PSEUDO_CONFIG(), pseudoSIDField.getPseudoConfig());
    }

    @Test
    void usesCustomPseudoConfigWhenKeysetIsSupplied() {
        int keySetPrimaryKey = 12345;

        EncryptedKeysetWrapper encryptedKeysetWrapper = mock(EncryptedKeysetWrapper.class);
        KeysetInfo keysetInfo = mock(KeysetInfo.class);

        when(encryptedKeysetWrapper.getKeysetInfo()).thenReturn(keysetInfo);
        when(keysetInfo.getPrimaryKeyId()).thenReturn(keySetPrimaryKey);

        PseudoSIDField pseudoSIDField = new PseudoSIDField("name", List.of("testValue"), encryptedKeysetWrapper);

        assertEquals(String.format("ff31(keyId=%d, strategy=SKIP)", keySetPrimaryKey),
                pseudoSIDField.getPseudoConfig().getRules().get(0).getFunc());
    }


    @Test
    void testPseudonymizeThenGetResponseField() {
        String fieldName = "name";
        List<String> fieldValues = List.of("1", "2", "3");
        List<String> expectedEncryptedFieldValues = List.of("encrypted:1", "encrypted:2", "encrypted:3");

        RecordMapProcessorFactory recordProcessorFactory = mock(RecordMapProcessorFactory.class);
        FieldPseudonymizer fieldPseudonymizer = mock(FieldPseudonymizer.class);

        when(recordProcessorFactory.newFieldPseudonymizer(anyList(), any())).thenReturn(fieldPseudonymizer);

        PseudoSIDField pseudoSIDField = spy(new PseudoSIDField("name", fieldValues, null));

        pseudoSIDField.setSidValues(fieldValues); // Setting sidValues equal to values for simplicity

        // Mock away pseudonymize, already tested in AbstractPseudFieldTest
        when(pseudoSIDField.pseudonymize(fieldValues, recordProcessorFactory)).thenReturn(expectedEncryptedFieldValues);

        ResponsePseudoField responsePseudoField = pseudoSIDField.pseudonymizeThenGetResponseField(recordProcessorFactory);

        //Response should contain the same fieldName
        assertEquals(fieldName, responsePseudoField.getFieldName());
        //Response should contain the same pseudoConfig
        assertEquals(pseudoSIDField.getPseudoConfig(), responsePseudoField.getPseudoRules());
        //Response encrypted values should be prefixed with "encrypted:"
        assertEquals(expectedEncryptedFieldValues, responsePseudoField.getValues());
    }

    @Test
    void testMapValueToSid() {
        String fieldName = "name";
        List<String> fieldValues = List.of("1", "2", "3");
        List<String> expectedSidValues = List.of("sid1", "sid2", "sid3");

        SidMapper sidMapper = mock(SidMapper.class);
        when(sidMapper.map(anyString())).thenAnswer(invocation -> "sid" + invocation.getArgument(0));

        PseudoSIDField pseudoSIDField = new PseudoSIDField(fieldName, fieldValues, null);
        pseudoSIDField.mapValueToSid(sidMapper);

        List<String> actualSidValues = pseudoSIDField.getSidValues();

        assertEquals(expectedSidValues, actualSidValues);
    }

}