package no.ssb.dlp.pseudo.service.filters;

import io.micronaut.context.propagation.slf4j.MdcPropagationContext;
import io.micronaut.core.propagation.MutablePropagatedContext;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.RequestFilter;
import io.micronaut.http.annotation.ResponseFilter;
import io.micronaut.http.annotation.ServerFilter;
import static io.micronaut.http.annotation.Filter.MATCH_ALL_PATTERN;
import io.micronaut.http.HttpRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import de.huxhorn.sulky.ulid.ULID;

import java.util.Optional;

@ServerFilter(MATCH_ALL_PATTERN)
@Slf4j
public class CorrelationIdFilter {
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String CORRELATION_ID_NAME = "CorrelationID";
    @RequestFilter
    public void correlationIdFilter(HttpRequest<?> request, MutablePropagatedContext mutablePropagatedContext) {
        String correlationID = Optional
                .ofNullable(request.getHeaders().get(CORRELATION_ID_HEADER))
                .orElse(new ULID().nextULID());

        MDC.put(CORRELATION_ID_NAME, correlationID);
        mutablePropagatedContext.add(new MdcPropagationContext());
        MDC.remove(CORRELATION_ID_NAME);
    }

    @ResponseFilter
    public void correlationIdHeaderFilter(MutableHttpResponse<?> response) {
        String header = MDC.get(CORRELATION_ID_NAME);
        response.getHeaders().add(CORRELATION_ID_HEADER, header);
    }
}