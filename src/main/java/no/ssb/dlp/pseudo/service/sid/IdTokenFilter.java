package no.ssb.dlp.pseudo.service.sid;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.IdToken;
import com.google.auth.oauth2.IdTokenProvider;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.filter.ClientFilterChain;
import io.micronaut.http.filter.HttpClientFilter;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;

import javax.inject.Singleton;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * This filter will obtain an {@link com.google.auth.oauth2.IdToken} and add it to the request. It uses Google's
 * default Application Default Credentials as opposed to the {@link io.micronaut.gcp.http.client.GoogleAuthFilter} which
 * uses the Compute metadata server.
 */
@IdTokenFilterMatcher
@Singleton
@Slf4j
public class IdTokenFilter implements HttpClientFilter {

    private final GoogleCredentials credentials;
    private final Map<String, IdToken> tokenCache = new HashMap<>();
    private final Duration expirationMargin = Duration.ofMinutes(5);
    public IdTokenFilter() {
        log.info("Using Google Credentials from Application Default Credentials");
        try {
            this.credentials = GoogleCredentials.getApplicationDefault();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Publisher<? extends HttpResponse<?>> doFilter(MutableHttpRequest<?> request, ClientFilterChain chain) {
        try {
            request.bearerAuth(getIdToken(request).getTokenValue());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return chain.proceed(request);
    }

    private IdToken getIdToken(MutableHttpRequest<?> request) throws IOException {
        final String audience = getAudienceFromRequest(request);
        IdToken token = tokenCache.computeIfAbsent(audience, this::newIdToken);
        Duration remaining = Duration.ofMillis(token.getExpirationTime().getTime() - System.currentTimeMillis());
        if (remaining.compareTo(expirationMargin) <= 0) {
            token = newIdToken(audience);
            tokenCache.replace(audience, token);
        }
        return token;
    }

    private IdToken newIdToken(String audience) {
        if (!(credentials instanceof IdTokenProvider)) {
            throw new IllegalArgumentException("Credentials are not an instance of IdTokenProvider.");
        }
        try {
            log.info("Getting IdToken for audience: " + audience);
            return ((IdTokenProvider) credentials).idTokenWithAudience(audience, null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getAudienceFromRequest(final MutableHttpRequest<?> request) {
        URI fullURI = request.getUri();
        return fullURI.getScheme() + "://" + fullURI.getHost();
    }

}
