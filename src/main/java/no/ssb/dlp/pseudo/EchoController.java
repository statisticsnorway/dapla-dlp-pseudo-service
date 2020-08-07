package no.ssb.dlp.pseudo;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import no.ssb.dlp.pseudo.service.security.PseudoServiceRole;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

/**
 * Temporary endpoint for use with testing the jupyter integration
 */
@Controller
@Secured(SecurityRule.IS_AUTHENTICATED)
@Slf4j
public class EchoController {

    @Post(value = "/echo", consumes = MediaType.APPLICATION_JSON)
    @Secured({PseudoServiceRole.ADMIN})
    public HttpResponse<Map<String, Object>> echo(@Body SomeRequest req, Principal principal) {
        Map<String, Object> res = new HashMap<>();
        res.put("username", principal.getName());
        res.put("request", req);
        return HttpResponse.ok(res);
    }

    @Data
    static class SomeRequest {
        private String someString;
        private Integer someInt;
        private Boolean someBool;
    }

}
