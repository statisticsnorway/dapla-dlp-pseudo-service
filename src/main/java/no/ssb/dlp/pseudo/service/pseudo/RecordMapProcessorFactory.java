package no.ssb.dlp.pseudo.service.pseudo;

import io.micronaut.context.event.ApplicationEventPublisher;
import lombok.RequiredArgsConstructor;
import no.ssb.dlp.pseudo.core.PseudoFuncRule;
import no.ssb.dlp.pseudo.core.PseudoKeyset;
import no.ssb.dlp.pseudo.core.field.FieldPseudonymizer;
import no.ssb.dlp.pseudo.core.field.ValueInterceptorChain;
import no.ssb.dlp.pseudo.core.map.RecordMapProcessor;
import no.ssb.dlp.pseudo.core.tink.model.EncryptedKeysetWrapper;
import no.ssb.dlp.pseudo.service.pseudo.metadata.FieldMetadataPublisher;
import no.ssb.dlp.pseudo.service.pseudo.metadata.PseudoMetadataEvent;

import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Singleton
public class RecordMapProcessorFactory {
    private final PseudoSecrets pseudoSecrets;

    private final ApplicationEventPublisher<PseudoMetadataEvent> pseudoMetadataPublisher;

    public RecordMapProcessor newPseudonymizeRecordProcessor(PseudoConfig... pseudoConfigs) {
        return newPseudonymizeRecordProcessor(Arrays.asList(pseudoConfigs));
    }

    public RecordMapProcessor newPseudonymizeRecordProcessor(List<PseudoConfig> pseudoConfigs) {
        ValueInterceptorChain chain = new ValueInterceptorChain()
                .register(new FieldMetadataPublisher(UUID.randomUUID().toString(), pseudoMetadataPublisher));

        for (PseudoConfig config : pseudoConfigs) {
            chain.register((f, v) -> newFieldPseudonymizer(config.getRules(), pseudoKeysetsOf(config.getKeysets())).pseudonymize(f, v));
        }

        return new RecordMapProcessor(chain);
    }

    public RecordMapProcessor newDepseudonymizeRecordProcessor(PseudoConfig... pseudoConfigs) {
        return newDepseudonymizeRecordProcessor(Arrays.asList(pseudoConfigs));
    }

    public RecordMapProcessor newDepseudonymizeRecordProcessor(List<PseudoConfig> pseudoConfigs) {
        ValueInterceptorChain chain = new ValueInterceptorChain();

        for (PseudoConfig config : pseudoConfigs) {
            chain.register((f, v) -> newFieldPseudonymizer(config.getRules(), pseudoKeysetsOf(config.getKeysets())).depseudonymize(f, v));
        }

        return new RecordMapProcessor(chain);
    }

    public RecordMapProcessor newRepseudonymizeRecordProcessor(PseudoConfig sourcePseudoConfig, PseudoConfig targetPseudoConfig) {
        return new RecordMapProcessor(
                new ValueInterceptorChain()
                        .register((f, v) -> newFieldPseudonymizer(sourcePseudoConfig.getRules(), pseudoKeysetsOf(sourcePseudoConfig.getKeysets())).depseudonymize(f, v))
                        .register((f, v) -> newFieldPseudonymizer(targetPseudoConfig.getRules(), pseudoKeysetsOf(targetPseudoConfig.getKeysets())).pseudonymize(f, v))
        );
    }

    private FieldPseudonymizer newFieldPseudonymizer(Collection<PseudoFuncRule> rules, Collection<PseudoKeyset> keysets) {
        return new FieldPseudonymizer.Builder()
                .secrets(pseudoSecrets.resolve())
                .rules(rules)
                .keysets(keysets)
                .build();
    }

    // TODO: This should not be needed
    private static List pseudoKeysetsOf(List<EncryptedKeysetWrapper> encryptedKeysets) {
        return encryptedKeysets.stream()
                .map(e -> (PseudoKeyset) e)
                .toList();
    }

}
