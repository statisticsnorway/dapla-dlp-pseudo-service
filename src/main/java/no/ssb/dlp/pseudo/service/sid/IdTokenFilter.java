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

/**
 * This filter will obtain an {@link com.google.auth.oauth2.IdToken} and add it to the request. It uses Google's
 * default Application Default Credentials as opposed to the {@link io.micronaut.gcp.http.client.GoogleAuthFilter} which
 * uses the Compute metadata server.
 */
@IdTokenFilterMatcher
@Singleton
@Slf4j
public class IdTokenFilter implements HttpClientFilter {
    @Override
    public Publisher<? extends HttpResponse<?>> doFilter(MutableHttpRequest<?> request, ClientFilterChain chain) {
        try {
            log.info("Using Google Credentials from Application Default Credentials");
            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
            if (!(credentials instanceof IdTokenProvider)) {
                throw new IllegalArgumentException("Credentials are not an instance of IdTokenProvider.");
            }
            IdToken idToken = ((IdTokenProvider) credentials).idTokenWithAudience(getAudienceFromRequest(request), null);
            request.bearerAuth(idToken.getTokenValue());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return chain.proceed(request);
    }

    private String getAudienceFromRequest(final MutableHttpRequest<?> request) {
        URI fullURI = request.getUri();
        return fullURI.getScheme() + "://" + fullURI.getHost();
    }

}
