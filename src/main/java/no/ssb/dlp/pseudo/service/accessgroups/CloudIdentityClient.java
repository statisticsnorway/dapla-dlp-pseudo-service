package no.ssb.dlp.pseudo.service.accessgroups;

import io.micronaut.http.annotation.Get;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import no.ssb.dlp.pseudo.service.filters.AccessTokenFilterMatcher;
import org.reactivestreams.Publisher;

import javax.annotation.Nullable;

@Client(id="cloud-identity-service")
@AccessTokenFilterMatcher()
public interface CloudIdentityClient {

    // https://cloud.google.com/identity/docs/reference/rest/v1/groups.memberships/list
    @Get( "/groups:lookup?groupKey.id={groupKeyId}")
    @ExecuteOn(TaskExecutors.IO)
    Publisher<LookupResponse> lookup(String groupKeyId);

    // https://cloud.google.com/identity/docs/reference/rest/v1/groups/lookup
    @Get( "/groups/{groupId}/memberships?pageToken={pageToken}")
    @ExecuteOn(TaskExecutors.IO)
    Publisher<MembershipResponse> listMembers(String groupId, @Nullable String pageToken);
}
