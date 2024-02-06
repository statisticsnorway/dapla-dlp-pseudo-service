package no.ssb.dlp.pseudo.service.pseudo;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.observers.TestObserver;
import io.reactivex.processors.ReplayProcessor;
import no.ssb.dlp.pseudo.core.PseudoOperation;
import no.ssb.dlp.pseudo.core.map.RecordMapProcessor;
import no.ssb.dlp.pseudo.core.tink.model.EncryptedKeysetWrapper;
import no.ssb.dlp.pseudo.service.pseudo.metadata.FieldMetadata;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static no.ssb.dlp.pseudo.service.pseudo.metadata.FieldMetadata.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@MicronautTest
class PseudoFieldTest {

    @Mock
    private PseudoConfigSplitter pseudoConfigSplitter;

    @Mock
    private RecordMapProcessorFactory recordProcessorFactory;

    @Mock
    private RecordMapProcessor<FieldMetadata> recordMapProcessor;

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
        when(recordProcessorFactory.newPseudonymizeRecordProcessor(any(), anyString())).thenReturn(recordMapProcessor);
        RecordMapProcessor.MetadataProcessor<FieldMetadata> metadataProcessorMock = mock(RecordMapProcessor.MetadataProcessor.class);
        final ReplayProcessor<FieldMetadata> publishProcessor = ReplayProcessor.create();
        publishProcessor.onNext(createFieldMetadata());
        when(metadataProcessorMock.toFlowableProcessor()).thenReturn(publishProcessor);
        when(recordMapProcessor.getMetadataProcessor()).thenReturn(metadataProcessorMock);
    }

    private static FieldMetadata createFieldMetadata() {
        return FieldMetadata.builder()
                .shortName("shortName")
                .dataElementPath("path")
                .dataElementPattern("pattern")
                .encryptionKeyReference("pattern")
                .encryptionAlgorithm("algorithm")
                .encryptionAlgorithmParameters(Map.of("key", "value"))
                .stableIdentifierVersion("stableIdVersion")
                .stableIdentifierType(STABLE_IDENTIFIER_TYPE)
                .build();
    }

    @Test
    void processWithNullValues() throws JSONException {
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

        String want = """
                {
                     "data": [
                       "processedValue v1",
                       null,
                       "processedValue v2"
                     ],
                     "metadata": [
                       {
                         "shortName": "shortName",
                         "dataElementPath": "path",
                         "dataElementPattern": "pattern",
                         "encryptionKeyReference": "pattern",
                         "encryptionAlgorithm": "algorithm",
                         "stableIdentifierVersion": "stableIdVersion",
                         "stableIdentifierType": "FREG_SNR",
                         "encryptionAlgorithmParameters": {
                           "key": "value"
                         }
                       }
                     ]
                }
                """;
        Flowable<String> result = pseudoField.process(pseudoConfigSplitter, recordProcessorFactory,
                values, PseudoOperation.PSEUDONYMIZE, "dummy-correlation-id");

        String got = String.join("", result.blockingIterable());

        JSONAssert.assertEquals(want, got, JSONCompareMode.STRICT);

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