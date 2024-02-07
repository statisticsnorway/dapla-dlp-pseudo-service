package no.ssb.dlp.pseudo.service.pseudo;

import lombok.RequiredArgsConstructor;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncInput;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncOutput;
import no.ssb.dapla.dlp.pseudo.func.fpe.FpeFunc;
import no.ssb.dapla.dlp.pseudo.func.map.MapFuncConfig;
import no.ssb.dapla.dlp.pseudo.func.tink.fpe.TinkFpeFunc;
import no.ssb.dlp.pseudo.core.PseudoException;
import no.ssb.dlp.pseudo.core.PseudoKeyset;
import no.ssb.dlp.pseudo.core.PseudoOperation;
import no.ssb.dlp.pseudo.core.field.FieldDescriptor;
import no.ssb.dlp.pseudo.core.field.ValueInterceptorChain;
import no.ssb.dlp.pseudo.core.func.PseudoFuncDeclaration;
import no.ssb.dlp.pseudo.core.func.PseudoFuncNames;
import no.ssb.dlp.pseudo.core.func.PseudoFuncRule;
import no.ssb.dlp.pseudo.core.func.PseudoFuncRuleMatch;
import no.ssb.dlp.pseudo.core.func.PseudoFuncs;
import no.ssb.dlp.pseudo.core.map.RecordMapProcessor;
import no.ssb.dlp.pseudo.core.tink.model.EncryptedKeysetWrapper;
import no.ssb.dlp.pseudo.service.pseudo.metadata.FieldMetadata;
import no.ssb.dlp.pseudo.service.pseudo.metadata.PseudoMetadataProcessor;

import javax.inject.Singleton;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static no.ssb.dlp.pseudo.core.PseudoOperation.DEPSEUDONYMIZE;
import static no.ssb.dlp.pseudo.core.PseudoOperation.PSEUDONYMIZE;
import static no.ssb.dlp.pseudo.core.func.PseudoFuncDeclaration.*;
import static no.ssb.dlp.pseudo.service.pseudo.metadata.FieldMetadata.*;

@RequiredArgsConstructor
@Singleton
public class RecordMapProcessorFactory {
    private final PseudoSecrets pseudoSecrets;

    public RecordMapProcessor<FieldMetadata> newPseudonymizeRecordProcessor(List<PseudoConfig> pseudoConfigs, String correlationId) {
        ValueInterceptorChain chain = new ValueInterceptorChain();
        PseudoMetadataProcessor metadataProcessor = new PseudoMetadataProcessor(correlationId);

        for (PseudoConfig config : pseudoConfigs) {
            final PseudoFuncs fieldPseudonymizer = newPseudoFuncs(config.getRules(),
                    pseudoKeysetsOf(config.getKeysets()));
            chain.preprocessor((f, v) -> init(fieldPseudonymizer, f, v));
            chain.register((f, v) -> process(PSEUDONYMIZE, fieldPseudonymizer, f, v, metadataProcessor));
        }
        return new RecordMapProcessor<>(chain, metadataProcessor::toFlowableProcessor);
    }

    public RecordMapProcessor<FieldMetadata> newDepseudonymizeRecordProcessor(List<PseudoConfig> pseudoConfigs, String correlationId) {
        ValueInterceptorChain chain = new ValueInterceptorChain();
        PseudoMetadataProcessor metadataProcessor = new PseudoMetadataProcessor(correlationId);

        for (PseudoConfig config : pseudoConfigs) {
            final PseudoFuncs fieldDepseudonymizer = newPseudoFuncs(config.getRules(),
                    pseudoKeysetsOf(config.getKeysets()));
            chain.register((f, v) -> process(DEPSEUDONYMIZE, fieldDepseudonymizer, f, v, metadataProcessor));
        }

        return new RecordMapProcessor<>(chain, metadataProcessor::toFlowableProcessor);
    }

    public RecordMapProcessor<FieldMetadata> newRepseudonymizeRecordProcessor(PseudoConfig sourcePseudoConfig,
                                                               PseudoConfig targetPseudoConfig, String correlationId) {
        final PseudoFuncs fieldDepseudonymizer = newPseudoFuncs(sourcePseudoConfig.getRules(),
                pseudoKeysetsOf(sourcePseudoConfig.getKeysets()));
        final PseudoFuncs fieldPseudonymizer = newPseudoFuncs(targetPseudoConfig.getRules(),
                pseudoKeysetsOf(targetPseudoConfig.getKeysets()));
        PseudoMetadataProcessor metadataProcessor = new PseudoMetadataProcessor(correlationId);
        return new RecordMapProcessor<>(
                new ValueInterceptorChain()
                        .register((f, v) -> process(DEPSEUDONYMIZE, fieldDepseudonymizer, f, v, metadataProcessor))
                        .register((f, v) -> process(PSEUDONYMIZE, fieldPseudonymizer, f, v, metadataProcessor)),
                metadataProcessor::toFlowableProcessor);
    }

    protected PseudoFuncs newPseudoFuncs(Collection<PseudoFuncRule> rules,
                                         Collection<PseudoKeyset> keysets) {
        return new PseudoFuncs(rules, pseudoSecrets.resolve(), keysets);
    }

    private String init(PseudoFuncs pseudoFuncs, FieldDescriptor field, String varValue) {
        if (varValue != null) {
            pseudoFuncs.findPseudoFunc(field).ifPresent(pseudoFunc ->
                    pseudoFunc.getFunc().init(PseudoFuncInput.of(varValue)));
        }
        return varValue;
    }

    private String process(PseudoOperation operation,
                           PseudoFuncs func,
                           FieldDescriptor field,
                           String varValue,
                           PseudoMetadataProcessor metadataProcessor) {
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
                PseudoFuncDeclaration funcDeclaration = PseudoFuncDeclaration.fromString(match.getRule().getFunc());
                final boolean isSidMapping = funcDeclaration.getFuncName().equals(PseudoFuncNames.MAP_SID);
                metadataProcessor.add(FieldMetadata.builder()
                        .shortName(field.getName())
                        .dataElementPath(field.getPath().substring(1).replace('/', '.')) // Skip leading slash and use dot as separator
                        .dataElementPattern(match.getRule().getPattern())
                        .encryptionKeyReference(isSidMapping ? null : funcDeclaration.getArgs().getOrDefault(KEY_REFERENCE, null))
                        .encryptionAlgorithm(match.getFunc().getAlgorithm())
                        .encryptionAlgorithmParameters(isSidMapping ? null : funcDeclaration.getArgs())
                        .stableIdentifierVersion(output.getMetadata().getOrDefault(MapFuncConfig.Param.SNAPSHOT_DATE, null))
                        .stableIdentifierType(isSidMapping ? STABLE_IDENTIFIER_TYPE : null)
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
