package no.ssb.dlp.pseudo.service.pseudo;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import no.ssb.dlp.pseudo.core.field.FieldDescriptor;
import no.ssb.dlp.pseudo.core.field.FieldPseudonymizer;
import no.ssb.dlp.pseudo.core.tink.model.EncryptedKeysetWrapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@MicronautTest
class PseudoFieldTest {

    @Test
    void UsesDefaultPseudoConfigWhenNoKeysetIsSupplied() {
        PseudoField pseudoField = new PseudoField(null, null, null, null);
        assertEquals(PseudoField.getDEFAULT_PSEUDO_FUNC(), pseudoField.getPseudoConfig().getRules().get(0).getFunc());
    }

    @Test
    void usesCustomPseudoFuncWhenPseudoFuncIsSupplied() {
        int keySetPrimaryKey = 12345;

        PseudoField pseudoSIDField = new PseudoField(null, null, String.format("map-sid(keyId=%s)", keySetPrimaryKey), null);

        assertEquals(String.format("map-sid(keyId=%s)", keySetPrimaryKey),
                pseudoSIDField.getPseudoConfig().getRules().get(0).getFunc());
    }

    @Test
    void setCustomKeysetWhenKeysetIsSupplied() {
        EncryptedKeysetWrapper encryptedKeysetWrapper = mock(EncryptedKeysetWrapper.class);
        PseudoField pseudoField = new PseudoField(null, null, null, encryptedKeysetWrapper);

        assertEquals(encryptedKeysetWrapper,
                pseudoField.getPseudoConfig().getKeysets().get(0));
    }

    @Test
    public void testPseudonymize() {
        List<String> fieldValues = List.of("1", "2", "3");
        List<String> expectedEncryptedFieldValues = List.of("encrypted:1", "encrypted:2", "encrypted:3");

        RecordMapProcessorFactory recordProcessorFactory = mock(RecordMapProcessorFactory.class);
        FieldPseudonymizer fieldPseudonymizer = mock(FieldPseudonymizer.class);

        when(recordProcessorFactory.newFieldPseudonymizer(anyList(), any())).thenReturn(fieldPseudonymizer);

        PseudoField pseudoField = new PseudoField(null, fieldValues, null, null);

        ArrayList<String> encryptedValues = new ArrayList<>();
        //Calls to fieldPseudonymizer pseudonymize will return "encrypted:" + original string
        when(fieldPseudonymizer.pseudonymize(any(), anyString()))
                .thenAnswer(invocation -> {
                    FieldDescriptor fieldDescriptor = invocation.getArgument(0);
                    String value = invocation.getArgument(1);
                    String encryptedValue = "encrypted:" + value;
                    encryptedValues.add(encryptedValue);
                    return encryptedValue;
                });

        assertEquals(expectedEncryptedFieldValues, pseudoField.pseudonymize(fieldValues, recordProcessorFactory));
    }

    @Test
    void testPseudonymizeThenGetResponseFieldWithDefaultConfig() {
        String fieldName = "name";
        List<String> fieldValues = List.of("1", "2", "3");
        List<String> expectedEncryptedFieldValues = List.of("encrypted:1", "encrypted:2", "encrypted:3");

        RecordMapProcessorFactory recordProcessorFactory = mock(RecordMapProcessorFactory.class);
        FieldPseudonymizer fieldPseudonymizer = mock(FieldPseudonymizer.class);
        when(recordProcessorFactory.newFieldPseudonymizer(anyList(), any())).thenReturn(fieldPseudonymizer);

        PseudoField pseudoField = spy(new PseudoField("name", fieldValues, null, null));

        // Mock away pseudonymize, tested in testPseudonymize
        when(pseudoField.pseudonymize(fieldValues, recordProcessorFactory)).thenReturn(expectedEncryptedFieldValues);

        ResponsePseudoField responsePseudoField = pseudoField.pseudonymizeThenGetResponseField(recordProcessorFactory);

        //Response should contain the same fieldName
        assertEquals(fieldName, responsePseudoField.getFieldName());
        //Response should contain the same pseudoConfig
        assertEquals(pseudoField.getPseudoConfig(), responsePseudoField.getPseudoRules());
        //Response encrypted values should be prefixed with "encrypted:"
        assertEquals(expectedEncryptedFieldValues, responsePseudoField.getValues());
    }

    @Test
    public void pseudonymizeTest() {
        List<String> fieldValues = List.of("1", "2", "3");
        List<String> expectedEncryptedFieldValues = List.of("encrypted:1", "encrypted:2", "encrypted:3");

        RecordMapProcessorFactory recordProcessorFactory = mock(RecordMapProcessorFactory.class);
        FieldPseudonymizer fieldPseudonymizer = mock(FieldPseudonymizer.class);

        when(recordProcessorFactory.newFieldPseudonymizer(anyList(), any())).thenReturn(fieldPseudonymizer);

        PseudoField pseudoField = new PseudoField(null, fieldValues, null, null);

        ArrayList<String> encryptedValues = new ArrayList<>();
        //Calls to fieldPseudonymizer pseudonymize will return "encrypted:" + original string
        when(fieldPseudonymizer.pseudonymize(any(), anyString()))
                .thenAnswer(invocation -> {
                    FieldDescriptor fieldDescriptor = invocation.getArgument(0);
                    String value = invocation.getArgument(1);
                    String encryptedValue = "encrypted:" + value;
                    encryptedValues.add(encryptedValue);
                    return encryptedValue;
                });

        assertEquals(expectedEncryptedFieldValues, pseudoField.pseudonymize(fieldValues, recordProcessorFactory));
    }

}