package no.ssb.dlp.pseudo.service.pseudo;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.observers.TestObserver;
import no.ssb.dlp.pseudo.core.map.RecordMapProcessor;
import no.ssb.dlp.pseudo.core.tink.model.EncryptedKeysetWrapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@MicronautTest
class PseudoFieldTest {

    @Mock
    private PseudoConfigSplitter pseudoConfigSplitter;

    @Mock
    private RecordMapProcessorFactory recordProcessorFactory;

    @Mock
    private RecordMapProcessor recordMapProcessor;

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

    void setUpProcessorMocks() {
        MockitoAnnotations.openMocks(this);
        when(pseudoConfigSplitter.splitIfNecessary(any())).thenReturn(Collections.singletonList(new PseudoConfig()));
        when(recordProcessorFactory.newPseudonymizeRecordProcessor(any())).thenReturn(recordMapProcessor);
    }

    @Test
    void processWithNullValues() {
        setUpProcessorMocks();

        //Preprocessor logic is covered in #preprocessorWithNullValues
        when(recordMapProcessor.hasPreprocessors()).thenReturn(false);

        when(recordMapProcessor.process(any())).thenAnswer(invocation -> {
            Map<String, String> argument = invocation.getArgument(0);
            String originalValue = argument.get("testField");
            return Collections.singletonMap("testField", "processedValue " + originalValue);
        });

        PseudoField pseudoField = new PseudoField("testField", null, null);
        List<String> values = Arrays.asList("v1", null, "v2");

        Flowable<List<Object>> result = pseudoField.process(pseudoConfigSplitter, recordProcessorFactory, values);
        List<List<Object>> resultList = result.toList().blockingGet();
        assertEquals(List.of(List.of("processedValue v1", Optional.empty(), "processedValue v2")), resultList);
        assertEquals(3, resultList.get(0).size());

        // Verify that recordMapProcessor was called once for each non-null value
        verify(recordMapProcessor, times(2)).process(any());
    }

    @Test
    void preprocessorWithNullValues() {
        setUpProcessorMocks();

        when(recordMapProcessor.hasPreprocessors()).thenReturn(true);
        when(recordMapProcessor.init(any())).thenReturn(Collections.singletonMap("testField", "initializedValue"));

        PseudoField pseudoField = new PseudoField("testField", null, null);
        List<String> values = Arrays.asList("v1", null, "v2");

        Completable result = pseudoField.getPreprocessor(values, recordMapProcessor);

        TestObserver<Void> testObserver = result.test();
        testObserver.assertComplete();
        testObserver.assertNoErrors();

        // Verify that recordMapProcessor was called once for each non-null value
        verify(recordMapProcessor, times(2)).init(any());
    }

}