package no.ssb.dlp.pseudo.service.pseudo;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.reactivex.Flowable;
import io.reactivex.processors.ReplayProcessor;
import no.ssb.dlp.pseudo.core.PseudoOperation;
import no.ssb.dlp.pseudo.core.map.RecordMapProcessor;
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

import static org.mockito.Mockito.*;

@MicronautTest
class DepseudoFieldTest {

    @Mock
    private PseudoConfigSplitter pseudoConfigSplitter;

    @Mock
    private RecordMapProcessorFactory recordProcessorFactory;

    @Mock
    private RecordMapProcessor<FieldMetadata> recordMapProcessor;

    void setUpProcessorMocks() {
        MockitoAnnotations.openMocks(this);
        when(pseudoConfigSplitter.splitIfNecessary(any())).thenReturn(Collections.singletonList(new PseudoConfig()));
        when(recordProcessorFactory.newDepseudonymizeRecordProcessor(any(), anyString())).thenReturn(recordMapProcessor);
        RecordMapProcessor.MetadataProcessor<FieldMetadata> metadataProcessorMock = mock(RecordMapProcessor.MetadataProcessor.class);
        final ReplayProcessor<FieldMetadata> publishProcessor = ReplayProcessor.create();
        publishProcessor.onNext(FieldMetadata.builder().path("path").name("testField").pattern("pattern").build());
        when(metadataProcessorMock.toFlowableProcessor()).thenReturn(publishProcessor);
        when(recordMapProcessor.getMetadataProcessor()).thenReturn(metadataProcessorMock);
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
                        "path": "path",
                        "name": "testField",
                        "pattern": "pattern",
                        "func": null,
                        "algorithm": null,
                        "metadata": null,
                        "warnings": null
                      }
                    ]
                  }
                """;
        Flowable<String> result = pseudoField.process(pseudoConfigSplitter, recordProcessorFactory,
                values, PseudoOperation.DEPSEUDONYMIZE, "dummy-correlation-id");

        String got = String.join("", result.blockingIterable());
        JSONAssert.assertEquals(want, got, JSONCompareMode.STRICT);

        // Verify that recordMapProcessor was called once for each non-null value
        verify(recordMapProcessor, times(2)).process(any());
    }
}