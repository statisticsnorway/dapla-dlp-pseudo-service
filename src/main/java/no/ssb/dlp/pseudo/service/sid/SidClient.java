package no.ssb.dlp.pseudo.service.sid;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import org.reactivestreams.Publisher;

@Client(id="sid-service")
@IdTokenFilterMatcher()
public interface SidClient {

    @Post("/sid/map")
    @ExecuteOn(TaskExecutors.BLOCKING)
    Publisher<SidInfo> lookup(@Body SidRequest sidRequest);

    @Post("/sid/map/batch")
    @ExecuteOn(TaskExecutors.BLOCKING)
    Publisher<MultiSidResponse> lookup(@Body MultiSidRequest multiSidRequest);

    @Get("/sid/snapshots")
    @ExecuteOn(TaskExecutors.BLOCKING)
    Publisher<SnapshotInfo> snapshots();

}
