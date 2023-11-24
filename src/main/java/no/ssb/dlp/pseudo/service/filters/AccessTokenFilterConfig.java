package no.ssb.dlp.pseudo.service.filters;

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import lombok.Data;

/**
 * Creates a GoogleAuthServiceConfig for each Service configured under
 * gcp.http.client.auth.services.*.audience. The audience can be configured per
 * service and the correct config bean is selected in {@code AccessTokenFilter} via the service id
 * inside the corresponding request.
 *
 * Requires the user to set the {@code gcp.http.client.auth.services.*.audience} property with the
 * desired audience to create the corresponding config bean.
 *
 */
@EachProperty(AccessTokenFilterConfig.PREFIX)
@Data
public class AccessTokenFilterConfig {
    public static final String PREFIX = "gcp.http.client.filter.services";

    private final String serviceId;

    public AccessTokenFilterConfig(@Parameter String serviceId) {
        this.serviceId = serviceId;
    }

    /**
     * @param audience set the desired audience
     */
    private String audience;
}
