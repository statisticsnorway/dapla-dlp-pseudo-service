package no.ssb.dlp.pseudo.service.pseudo;

import io.micronaut.context.event.ApplicationEventPublisher;
import lombok.RequiredArgsConstructor;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncInput;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncOutput;
import no.ssb.dapla.dlp.pseudo.func.fpe.FpeFunc;
import no.ssb.dapla.dlp.pseudo.func.tink.fpe.TinkFpeFunc;
import no.ssb.dlp.pseudo.core.PseudoException;
import no.ssb.dlp.pseudo.core.PseudoKeyset;
import no.ssb.dlp.pseudo.core.PseudoOperation;
import no.ssb.dlp.pseudo.core.field.FieldDescriptor;
import no.ssb.dlp.pseudo.core.field.ValueInterceptorChain;
import no.ssb.dlp.pseudo.core.func.PseudoFuncRule;
import no.ssb.dlp.pseudo.core.func.PseudoFuncRuleMatch;
import no.ssb.dlp.pseudo.core.func.PseudoFuncs;
import no.ssb.dlp.pseudo.core.map.RecordMapProcessor;
import no.ssb.dlp.pseudo.core.tink.model.EncryptedKeysetWrapper;
import no.ssb.dlp.pseudo.service.pseudo.metadata.PseudoMetadataEvent;
import no.ssb.dlp.pseudo.service.pseudo.metadata.PseudoMetadataEventListener;

import javax.inject.Singleton;
import java.util.Collection;
import java.util.List;

import static no.ssb.dlp.pseudo.core.PseudoOperation.DEPSEUDONYMIZE;
import static no.ssb.dlp.pseudo.core.PseudoOperation.PSEUDONYMIZE;

@RequiredArgsConstructor
@Singleton
public class RecordMapProcessorFactory {
    private final PseudoSecrets pseudoSecrets;

    public RecordMapProcessor newPseudonymizeRecordProcessor(List<PseudoConfig> pseudoConfigs, String correlationId) {
        ValueInterceptorChain chain = new ValueInterceptorChain();
        PseudoMetadataEventListener metadataEventListener = new PseudoMetadataEventListener(correlationId);

        for (PseudoConfig config : pseudoConfigs) {
            final PseudoFuncs fieldPseudonymizer = newPseudoFuncs(config.getRules(),
                    pseudoKeysetsOf(config.getKeysets()), correlationId);
            chain.preprocessor((f, v) -> init(fieldPseudonymizer, f, v));
            chain.register((f, v) -> process(PSEUDONYMIZE, fieldPseudonymizer, f, v, metadataEventListener));
        }
        return new RecordMapProcessor(chain, metadataEventListener::getMetadataProcessor);
    }

    public RecordMapProcessor newDepseudonymizeRecordProcessor(List<PseudoConfig> pseudoConfigs, String correlationId) {
        ValueInterceptorChain chain = new ValueInterceptorChain();
        PseudoMetadataEventListener metadataEventListener = new PseudoMetadataEventListener(correlationId);

        for (PseudoConfig config : pseudoConfigs) {
            final PseudoFuncs fieldDepseudonymizer = newPseudoFuncs(config.getRules(),
                    pseudoKeysetsOf(config.getKeysets()), correlationId);
            chain.register((f, v) -> process(DEPSEUDONYMIZE, fieldDepseudonymizer, f, v, metadataEventListener));
        }

        return new RecordMapProcessor(chain, metadataEventListener::getMetadataProcessor);
    }

    public RecordMapProcessor newRepseudonymizeRecordProcessor(PseudoConfig sourcePseudoConfig,
                                                               PseudoConfig targetPseudoConfig, String correlationId) {
        final PseudoFuncs fieldDepseudonymizer = newPseudoFuncs(sourcePseudoConfig.getRules(),
                pseudoKeysetsOf(sourcePseudoConfig.getKeysets()), correlationId);
        final PseudoFuncs fieldPseudonymizer = newPseudoFuncs(targetPseudoConfig.getRules(),
                pseudoKeysetsOf(targetPseudoConfig.getKeysets()), correlationId);
        PseudoMetadataEventListener metadataEventListener = new PseudoMetadataEventListener(correlationId);
        return new RecordMapProcessor(
                new ValueInterceptorChain()
                        .register((f, v) -> process(DEPSEUDONYMIZE, fieldDepseudonymizer, f, v, metadataEventListener))
                        .register((f, v) -> process(PSEUDONYMIZE, fieldPseudonymizer, f, v, metadataEventListener)),
                metadataEventListener::getMetadataProcessor);
    }

    protected PseudoFuncs newPseudoFuncs(Collection<PseudoFuncRule> rules,
                                         Collection<PseudoKeyset> keysets,
                                         String correlationId) {
        return new PseudoFuncs(rules, pseudoSecrets.resolve(), keysets, correlationId);
    }

    private String init(PseudoFuncs pseudoFuncs, FieldDescriptor field, String varValue) {
        pseudoFuncs.findPseudoFunc(field).ifPresent(pseudoFunc ->
                pseudoFunc.getFunc().init(PseudoFuncInput.of(varValue)));
        return varValue;
    }

    private String process(PseudoOperation operation,
                           PseudoFuncs func,
                           FieldDescriptor field,
                           String varValue,
                           PseudoMetadataEventListener eventListener) {
        PseudoFuncRuleMatch match = func.findPseudoFunc(field).orElse(null);

        if (varValue == null || match == null) {
            return varValue;
        }
        // Due to FPE limitations, we can not pseudonymize values shorter than 2 characters
        if (varValue.length() <= 2 && (match.getFunc() instanceof FpeFunc || match.getFunc() instanceof TinkFpeFunc)) {
            return varValue;
        }
        try {
            PseudoFuncOutput output;
            if (operation == PSEUDONYMIZE) {
                output = match.getFunc().apply(PseudoFuncInput.of(varValue));
                eventListener.onPseudoMetadataEvent(PseudoMetadataEvent.builder()
                        .field(field)
                        .rule(match.getRule())
                        .algorithm(match.getFunc().getAlgorithm())
                        .metadata(output.getMetadata())
                        .warnings(output.getWarnings())
                        .build());
            } else {
                output = match.getFunc().restore(PseudoFuncInput.of(varValue));
            }
            return (String) output.getFirstValue();
        } catch (Exception e) {
            throw new PseudoException(String.format("pseudonymize error - field='%s', originalValue='%s'",
                    field.getPath(), varValue), e);
        }
    }


    // TODO: This should not be needed
    protected static List<PseudoKeyset> pseudoKeysetsOf(List<EncryptedKeysetWrapper> encryptedKeysets) {
        return encryptedKeysets.stream()
                .map(e -> (PseudoKeyset) e)
                .toList();
    }

}
