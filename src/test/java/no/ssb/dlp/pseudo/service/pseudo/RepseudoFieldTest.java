package no.ssb.dlp.pseudo.service.pseudo;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.observers.TestObserver;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.processors.ReplayProcessor;
import no.ssb.dlp.pseudo.core.PseudoOperation;
import no.ssb.dlp.pseudo.core.map.RecordMapProcessor;
import no.ssb.dlp.pseudo.core.tink.model.EncryptedKeysetWrapper;
import no.ssb.dlp.pseudo.core.util.Json;
import no.ssb.dlp.pseudo.service.pseudo.metadata.FieldMetadata;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@MicronautTest
class RepseudoFieldTest {

    @Mock
    private PseudoConfigSplitter pseudoConfigSplitter;

    @Mock
    private RecordMapProcessorFactory recordProcessorFactory;

    @Mock
    private RecordMapProcessor<FieldMetadata> recordMapProcessor;
    void setUpProcessorMocks() {
        MockitoAnnotations.openMocks(this);
        when(pseudoConfigSplitter.splitIfNecessary(any())).thenReturn(Collections.singletonList(new PseudoConfig()));
        when(recordProcessorFactory.newRepseudonymizeRecordProcessor(any(), any(), anyString())).thenReturn(recordMapProcessor);
        RecordMapProcessor.MetadataProcessor<FieldMetadata> metadataProcessorMock = mock(RecordMapProcessor.MetadataProcessor.class);
        final ReplayProcessor<FieldMetadata> publishProcessor = ReplayProcessor.create();
        publishProcessor.onNext(FieldMetadata.builder().path("path").name("testField").pattern("pattern").build());
        when(metadataProcessorMock.toFlowableProcessor()).thenReturn(publishProcessor);
        when(recordMapProcessor.getMetadataProcessor()).thenReturn(metadataProcessorMock);
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

        PseudoField sourcePseudoField = new PseudoField("testField", null, null);
        PseudoField targetPseudoField = new PseudoField("testField", null, null);
        List<String> values = Arrays.asList("v1", null, "v2");

        Flowable<String> result = sourcePseudoField.process(recordProcessorFactory,
                values, targetPseudoField, "dummy-correlation-id");
        List<String> resultList = result.toList().blockingGet();
        assertEquals(List.of(List.of("processedValue v1", Optional.empty(), "processedValue v2")), resultList);
        assertEquals(3, resultList.size());

        // Verify that recordMapProcessor was called once for each non-null value
        verify(recordMapProcessor, times(2)).process(any());
    }
}