package no.ssb.dlp.pseudo.service.keys;

import com.google.common.base.Strings;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.hateoas.JsonError;
import io.micronaut.http.hateoas.Link;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.ssb.dlp.pseudo.core.tink.model.EncryptedKeysetWrapper;

import java.net.URI;
import java.security.Principal;

@Introspected
@RequiredArgsConstructor
@Controller("/keys")
@Slf4j
@Secured(SecurityRule.IS_AUTHENTICATED)
@Tag(name = "Key operations")
public class KeyController {

    private final KeyService keyService;

    @Post
    public HttpResponse<EncryptedKeysetWrapper> generateDataEncryptionKey(KeyGenerationRequest request, Principal principal) {
        log.info(Strings.padEnd(String.format("*** Generate data encryption key"), 80, '*'));
        log.debug("User: {}\n{}", principal.getName(), request);
        EncryptedKeysetWrapper keyset = keyService.createNewDataEncryptionKey(request.getKekUri());
        return HttpResponse.ok(keyset);
    }

    @Data
    static class KeyGenerationRequest {
        private URI kekUri;
    }

    @Error
    public HttpResponse<JsonError> unsupportedKeyEncryptionKeyError(HttpRequest request, KeyService.UnsupportedKeyException e) {
        JsonError error = new JsonError(e.getMessage())
                .link(Link.SELF, Link.of(request.getUri()));
        return HttpResponse.<JsonError>badRequest().body(error);
    }

}
