package no.ssb.dlp.pseudo.service.keys;

import com.google.common.base.Strings;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.hateoas.JsonError;
import io.micronaut.http.hateoas.Link;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.ssb.dlp.pseudo.core.tink.model.EncryptedKeysetWrapper;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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

    @Operation(
            summary = "Generate Wrapped DEK",
            description = """
            Generate a custom Wrapped Data Encryption Key (WDEK) that can be used for pseudonymization.
            
            The generated key will be encrypted (a process known as "wrapping") by the user-specified KEK
            (Key Encryption Key). If a `kekUri` is not specified, then the default KEK used. Relying on the
            default KEK is the preferred default usage when generating new DEKs.
            """
    )
    @Post
    public HttpResponse<EncryptedKeysetWrapper> generateWrappedDataEncryptionKey(@Valid KeyGenerationRequest request, Principal principal) {
        log.info(Strings.padEnd(String.format("*** Generate data encryption key ***"), 80, '*'));
        log.debug("User: {}", principal.getName());
        EncryptedKeysetWrapper keyset = keyService.createNewDataEncryptionKey(request.getKekUri(), request.getKeyTemplateName());
        log.debug("Generated key:\n{}", keyset.toJson());
        return HttpResponse.ok(keyset);
    }

    @Data
    @Introspected
    static class KeyGenerationRequest {
        /**
         * Name of the key template to use when generating key material.
         * <p>
         * E.g. AES256_SIV or FPE_FF31_256_ALPHANUMERIC, or see {@link no.ssb.crypto.tink.fpe.FpeFfxKeyType}) for more.
         * </p>
         */
        @NotNull
        private String keyTemplateName;

        /**
         * URI to the key encryption key stored in KMS.
         * <p>
         * E.g. gcp-kms://projects/my-project-id/locations/europe-north1/keyRings/my-keyring/cryptoKeys/my-kek
         * </p>
         */
        private URI kekUri;
    }

    @Error
    public HttpResponse<JsonError> unsupportedKeyEncryptionKeyError(HttpRequest request, KeyService.UnsupportedKeyException e) {
        JsonError error = new JsonError(e.getMessage())
                .link(Link.SELF, Link.of(request.getUri()));
        return HttpResponse.<JsonError>badRequest().body(error);
    }

}
