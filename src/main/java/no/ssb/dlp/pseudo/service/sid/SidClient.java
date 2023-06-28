package no.ssb.dlp.pseudo.service.sid;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import org.reactivestreams.Publisher;

import java.util.Map;

@Client(id="sid-service")
@IdTokenFilterMatcher()
public interface SidClient {

    @Post( "/sid/map")
    @ExecuteOn(TaskExecutors.IO)
    Publisher<SidInfo> lookup(@Body SidRequest sidRequest);

    @Post( "/sid/map/batch")
    @ExecuteOn(TaskExecutors.IO)
    Publisher<Map<String, SidInfo>> lookup(@Body MultiSidRequest multiSidRequest);

}
