package no.ssb.dlp.pseudo.service.sid;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;
import org.reactivestreams.Publisher;

@Client(id="sid-service")
@IdTokenFilterMatcher()
public interface SidClient {

    @Post( "/sid/map")
    Publisher<SidInfo> lookup(@Body SidRequest sidRequest);
}
