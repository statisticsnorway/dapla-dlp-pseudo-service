package no.ssb.dlp.pseudo.service;

import lombok.RequiredArgsConstructor;
import no.ssb.avro.convert.core.FieldDescriptor;
import no.ssb.dapla.dlp.pseudo.func.PseudoFunc;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncInput;
import no.ssb.dapla.parquet.FieldInterceptor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Intercept all fields, determine if they should be (de)pseudonymized and perform the (de)pseudonymization where
 * appropriate.
 */
@RequiredArgsConstructor
public class FieldPseudoInterceptor implements FieldInterceptor {

    // Map fields to functions to avoid doing pattern matching on all fields
    private final Map<String, PseudoFunc> fieldToFunc = new HashMap<>();

    // Keep track of which fields shouldn't be (de)pseudonymized to avoid excessive pattern matching
    private final Set<String> noOpFields = new HashSet<>();

    private final PseudoFuncs funcs;

    private final PseudoOperation operation;

    @Override
    public String intercept(String field, String value) {
        return getPseudoFieldInterceptor(field, operation).intercept(field, value);
    }

    private FieldInterceptor getPseudoFieldInterceptor(String field, PseudoOperation pseudoOperation) {
        if (noOpFields.contains(field)) {
            // We've previously determined that this field shouldn't be (de)pseudonymized
            return FieldInterceptor.noOp();
        } else if (fieldToFunc.containsKey(field)) {
            // We've previously determined that this field should be (de)pseudonymized
            return funcToInterceptor(fieldToFunc.get(field), pseudoOperation);
        } else {
            // First time encountering this field, determine if it should be (de)pseudonymized
            FieldInterceptor interceptor = FieldInterceptor.noOp();
            Optional<PseudoFuncRuleMatch> match = funcs.findPseudoFunc(new FieldDescriptor(field));
            if (match.isPresent()) {
                PseudoFunc func = match.get().getFunc();
                fieldToFunc.put(field, func);
                interceptor = funcToInterceptor(func, pseudoOperation);
            } else {
                noOpFields.add(field);
            }
            return interceptor;
        }
    }

    private static FieldInterceptor funcToInterceptor(PseudoFunc func, PseudoOperation pseudoOperation) {
        return pseudoOperation == PseudoOperation.PSEUDONYMIZE ?
                (f, v) -> (String) func.apply(PseudoFuncInput.of(v)).getFirstValue() :
                (f, v) -> (String) func.restore(PseudoFuncInput.of(v)).getFirstValue();
    }
}
