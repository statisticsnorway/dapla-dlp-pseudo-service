package no.ssb.dlp.pseudo.service.pseudo;

import lombok.Data;
import lombok.extern.log4j.Log4j2;
import no.ssb.dlp.pseudo.core.field.FieldDescriptor;
import no.ssb.dlp.pseudo.core.field.FieldPseudonymizer;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for pseudo fields.
 *
 * @param <T> The type of the response field.
 */
@Data
@Log4j2
public abstract class AbstractPseudoField<T extends ResponsePseudoField> {
    protected String name;
    protected List<String> values;
    protected PseudoConfig pseudoConfig;

    AbstractPseudoField(String name, List<String> values) {
        this.name = name;
        this.values = values;
    }

    protected abstract T pseudonymizeThenGetResponseField(RecordMapProcessorFactory recordProcessorFactory);

    protected abstract T getResponseField(List<String> encryptedValues);

    /**
     * Pseudonymizes the provided values using the given record processor factory.
     *
     * @param values                 The values to be pseudonymized.
     * @param recordProcessorFactory The record processor factory used to create a field pseudonymizer.
     * @return The pseudonymized values.
     */
    protected List<String> pseudonymize(List<String> values, RecordMapProcessorFactory recordProcessorFactory) {
        Instant startTime = Instant.now();

        FieldPseudonymizer fieldPseudonymizer = recordProcessorFactory.newFieldPseudonymizer(this.getPseudoConfig().getRules(), RecordMapProcessorFactory.pseudoKeysetsOf(this.getPseudoConfig().getKeysets()));

        ArrayList<String> encryptedValues = new ArrayList<>();

        values.stream().map(value -> fieldPseudonymizer.pseudonymize(new FieldDescriptor(this.getName()), value)).forEach(result -> encryptedValues.add(result));

        Instant endTime = Instant.now();
        Duration duration = Duration.between(startTime, endTime);
        log.info("Pseudonymizing field '{}' took {} milliseconds.", this.getName(), duration.toMillis());

        return encryptedValues;
    }

}