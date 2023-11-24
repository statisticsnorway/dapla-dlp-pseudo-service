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
