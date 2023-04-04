package no.ssb.dlp.pseudo.service.sid;

import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.token.generator.TokenGenerator;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.restassured.specification.RequestSpecification;
import no.ssb.dlp.pseudo.service.security.PseudoServiceRole;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


/**
 * Test of {@link SidLookupController} by mocking response from {@link SidClient}
 */
@MicronautTest
public class SidLookupControllerTest {

    @Inject
    SidClient sidClient;

    @Inject
    TokenGenerator tokenGenerator;
    @Inject
    @Client("/")
    HttpClient client;

    @Test
    public void testLookupFnr(RequestSpecification spec) {
        Authentication user = Authentication.build("sherlock", Set.of(PseudoServiceRole.ADMIN));
        Optional<String> accessToken = tokenGenerator.generateToken(user, 10000);
        assertTrue(accessToken.isPresent());

        when(sidClient.lookup(any(SidRequest.class))).thenReturn(Publishers.just(
                new SidInfo.SidInfoBuilder().currentFnr("11854898347").currentSnr("0001ha3").build())
        );
        spec.when().auth().oauth2(accessToken.get())
            .get("/sid/fnr/{fnr}", "11854898347")
        .then()
            .statusCode(200)
            .body(containsString("11854898347"))
            .body(containsString("0001ha3"));
        verify(sidClient, times(1)).lookup(any(SidRequest.class));
    }
    @Test
    public void testLookupSnr(RequestSpecification spec) {
        Authentication user = Authentication.build("sherlock", Set.of(PseudoServiceRole.ADMIN));
        Optional<String> accessToken = tokenGenerator.generateToken(user, 10000);
        assertTrue(accessToken.isPresent());

        when(sidClient.lookup(any(SidRequest.class))).thenReturn(Publishers.just(
                new SidInfo.SidInfoBuilder().currentFnr("11854898347").currentSnr("0001ha3").build())
        );

        spec.when().auth().oauth2(accessToken.get())
                .get("/sid/snr/{snr}", "0001ha3")
                .then()
                .statusCode(200)
                .body(containsString("11854898347"))
                .body(containsString("0001ha3"));
        verify(sidClient, times(1)).lookup(any(SidRequest.class));
    }

    @MockBean(SidClient.class)
    SidClient sidClient() {
        return mock(SidClient.class);
    }
}
