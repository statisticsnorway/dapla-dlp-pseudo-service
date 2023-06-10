package no.ssb.dlp.pseudo.service.pseudo;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import no.ssb.dlp.pseudo.core.field.FieldDescriptor;
import no.ssb.dlp.pseudo.core.field.FieldPseudonymizer;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest
class AbstractPseudoFieldTest {
    @Test
    public void pseudonymizeTest() {
        List<String> fieldValues = List.of("1", "2", "3");
        List<String> expectedEncryptedFieldValues = List.of("encrypted:1", "encrypted:2", "encrypted:3");

        RecordMapProcessorFactory recordProcessorFactory = mock(RecordMapProcessorFactory.class);
        FieldPseudonymizer fieldPseudonymizer = mock(FieldPseudonymizer.class);

        when(recordProcessorFactory.newFieldPseudonymizer(anyList(), any())).thenReturn(fieldPseudonymizer);

        AbstractPseudoField pseudoField = new PseudoField(null, fieldValues, null);

        ArrayList<String> encryptedValues = new ArrayList<>();
        when(fieldPseudonymizer.pseudonymize(any(), anyString()))
                .thenAnswer(invocation -> {
                    FieldDescriptor fieldDescriptor = invocation.getArgument(0);
                    String value = invocation.getArgument(1);
                    // Perform any necessary logic to mock the pseudonymization
                    String encryptedValue = "encrypted:" + value;
                    encryptedValues.add(encryptedValue);
                    return encryptedValue;
                });

        assertEquals(expectedEncryptedFieldValues, pseudoField.pseudonymize(fieldValues, recordProcessorFactory));
    }
}