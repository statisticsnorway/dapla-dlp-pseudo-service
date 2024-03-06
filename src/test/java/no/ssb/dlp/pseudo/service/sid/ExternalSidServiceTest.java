package no.ssb.dlp.pseudo.service.sid;

import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

@MicronautTest
public class ExternalSidServiceTest {

    @Inject
    SidService sidService;
    @Inject
    SidClient sidClient;

    @Test
    public void testInvokeSingleFnr() {
        // sidService should call our sidClient mock
        when(sidClient.lookup(any(SidRequest.class))).thenReturn(Publishers.just(
                SidInfo.builder().fnr("11854898347").snr("0001ha3").build())
        );
        sidService.lookupFnr("11854898347", Optional.ofNullable(null));

        verify(sidClient, times(1)).lookup(any(SidRequest.class));
    }

    @Test
    public void testInvokeMultiFnr() {
        // sidService should call our sidClient mock
        when(sidClient.lookup(any(MultiSidRequest.class))).thenReturn(Publishers.just(
                MultiSidResponse.builder().mapping(MultiSidResponse.Mapping.builder()
                        .fnrList(Arrays.asList("11854898347"))
                        .fnr(Arrays.asList("11854898347"))
                        .snr(Arrays.asList("0001ha3"))
                        .build()).build())
        );
        sidService.lookupFnr(List.of("11854898347"), Optional.ofNullable(null));

        verify(sidClient, times(1)).lookup(any(MultiSidRequest.class));
    }

    @Test
    public void testInvokeSingleSnr() {
        when(sidClient.lookup(any(SidRequest.class))).thenReturn(Publishers.just(
                SidInfo.builder().snr("0001ha3").fnr("11854898347").build())
        );
        sidService.lookupSnr("0001ha3", Optional.ofNullable(null));

        verify(sidClient, times(1)).lookup(any(SidRequest.class));
    }

    @Test
    public void testInvokeMultiSnr() {
        // sidService should call our sidClient mock
        when(sidClient.lookup(any(MultiSidRequest.class))).thenReturn(Publishers.just(
                MultiSidResponse.builder().mapping(MultiSidResponse.Mapping.builder()
                        .snr(List.of("0001ha3", "0006kh2"))
                        .fnr(List.of("11854898347"))
                        .build()).build())
        );
        sidService.lookupSnr(List.of("0001ha3"), Optional.ofNullable(null));

        verify(sidClient, times(1)).lookup(any(MultiSidRequest.class));
    }

    @MockBean(SidClient.class)
    SidClient sidClient() {
        return mock(SidClient.class);
    }
}
