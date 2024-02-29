package no.ssb.dlp.pseudo.service.accessgroups;

import io.micronaut.http.annotation.Get;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.annotation.Nullable;
import no.ssb.dlp.pseudo.service.filters.AccessTokenFilterMatcher;
import org.reactivestreams.Publisher;

@Client(id="cloud-identity-service")
@AccessTokenFilterMatcher()
public interface CloudIdentityClient {

    /**
     * Lookup a group by its email address.
     * See: https://cloud.google.com/identity/docs/reference/rest/v1/groups/lookup
     * @param groupKeyId the email address of the group
     * @return a {@link Publisher} of {@link LookupResponse}
     */
    @Get( "/groups:lookup?groupKey.id={groupKeyId}")
    @ExecuteOn(TaskExecutors.IO)
    Publisher<LookupResponse> lookup(String groupKeyId);

    /**
     * List all members of a group.
     * See: https://cloud.google.com/identity/docs/reference/rest/v1/groups.memberships/list
     *
     * @param groupId the id of the group
     * @param pageToken for pagination
     * @return a {@link Publisher} of {@link MembershipResponse}
     */
    @Get( "/groups/{groupId}/memberships?pageToken={pageToken}")
    @ExecuteOn(TaskExecutors.IO)
    Publisher<MembershipResponse> listMembers(String groupId, @Nullable String pageToken);
}
