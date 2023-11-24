package no.ssb.dlp.pseudo.service.accessgroups;

import io.micronaut.cache.annotation.Cacheable;
import io.reactivex.Flowable;
import lombok.RequiredArgsConstructor;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
@RequiredArgsConstructor
public class CloudIdentityService {
    private final CloudIdentityClient cloudIdentityClient;

    @Cacheable(value = "cloud-identity-service-cache", parameters = {"groupEmail"})
    public List<Membership> listMembers(String groupEmail) {
        return Flowable.fromPublisher(cloudIdentityClient.lookup(groupEmail))
                .flatMap(lookupResponse -> fetchMemberships(lookupResponse.getGroupName(), null,
                        new ArrayList<>()))
                .blockingFirst();
    }

    /**
     * Paginate through all memberships of a group.
     *
     * @param groupId the id of the group
     * @param nextPageToken a token for pagination (will be null on first call)
     * @param allMemberships a list that will be populated with all memberships
     * @return the list of all memberships
     */
    private Flowable<List<Membership>> fetchMemberships(String groupId, String nextPageToken,
                                                                   List<Membership> allMemberships) {
        if (groupId == null || groupId.isEmpty()) {
            return Flowable.just(allMemberships);
        }
        return Flowable.fromPublisher(cloudIdentityClient.listMembers(groupId, nextPageToken))
                .flatMap(membershipResponse -> {
                    allMemberships.addAll(membershipResponse.getMemberships());
                    String nextToken = membershipResponse.getNextPageToken();
                    return nextToken != null ?
                            fetchMemberships(groupId, nextToken, allMemberships) :
                            Flowable.just(allMemberships);
                });
    }
}
