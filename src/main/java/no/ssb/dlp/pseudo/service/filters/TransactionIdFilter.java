package no.ssb.dlp.pseudo.service.filters;

import io.micronaut.context.propagation.slf4j.MdcPropagationContext;
import io.micronaut.core.propagation.MutablePropagatedContext;
import io.micronaut.http.annotation.RequestFilter;
import io.micronaut.http.annotation.ServerFilter;
import static io.micronaut.http.annotation.Filter.MATCH_ALL_PATTERN;
import io.micronaut.http.HttpRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import de.huxhorn.sulky.ulid.ULID;

import java.util.Optional;

@ServerFilter(MATCH_ALL_PATTERN)
@Slf4j
public class TransactionIdFilter {
    @RequestFilter
    public void transactionIdFilter(HttpRequest<?> request, MutablePropagatedContext mutablePropagatedContext) {
        ULID.Value correlationID = Optional
                .ofNullable(request.getHeaders().get("X-Correlation-Id"))
                .map(ULID::parseULID)
                .orElse(new ULID().nextValue());

        log.info("Request CorrelationID: " + correlationID.toString());
        MDC.put("CorrelationID", correlationID.toString());

        mutablePropagatedContext.add(new MdcPropagationContext());
        MDC.remove("CorrelationID");
    }
}