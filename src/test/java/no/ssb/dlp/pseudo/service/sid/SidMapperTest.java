package no.ssb.dlp.pseudo.service.sid;

import io.micronaut.context.ApplicationContext;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncInput;
import no.ssb.dapla.dlp.pseudo.func.map.Mapper;
import no.ssb.dlp.pseudo.service.Application;
import org.apache.groovy.util.Maps;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

import static org.mockito.Mockito.*;

/**
 * Test {@link SidMapper} indirectly via the JDK's ServiceLoader. This requires static mocking of the
 * {@link Application} class.
 */
@MicronautTest
public class SidMapperTest {

    @Inject
    SidService sidService;
    @Inject
    ApplicationContext context;

    @Test
    public void testInvokeMapperFunc() {
        when(sidService.lookupFnr(anyList(), any(Optional.class))).thenReturn(Publishers.just(
                Maps.of("11854898347", new SidInfo.SidInfoBuilder().snr("0001ha3").build()))
        );
        // Use static mocking to override the application context
        try (var application = mockStatic(Application.class)) {
            application.when(Application::getContext).thenReturn(context);
            Mapper mapper = ServiceLoader.load(Mapper.class).findFirst().orElseThrow(() ->
                    new RuntimeException("SidMapper class not found"));
            mapper.setConfig(new HashMap<>());
            mapper.init(PseudoFuncInput.of("11854898347"));
            Object mappedSid = mapper.map(PseudoFuncInput.of("11854898347")).getFirstValue();
            verify(sidService, times(1)).lookupFnr(anyList(), eq(Optional.ofNullable(null)));
            Assertions.assertEquals("0001ha3", mappedSid);
        }
    }

    @Test
    public void testInvokeRestoreFunc(){
        when(sidService.lookupSnr(anyList(), any(Optional.class))).thenReturn(Publishers.just(
                Maps.of("0001ha3", new SidInfo.SidInfoBuilder().fnr("11854898347").build()))
        );
        // Use static mocking to override the application context
        try (var application = mockStatic(Application.class)) {
            application.when(Application::getContext).thenReturn(context);
            Mapper mapper = ServiceLoader.load(Mapper.class).findFirst().orElseThrow(() ->
                    new RuntimeException("SidMapper class not found"));
            mapper.setConfig(new HashMap<>());
            mapper.init(PseudoFuncInput.of("0001ha3"));
            Object mappedSid = mapper.restore(PseudoFuncInput.of("0001ha3")).getFirstValue();
            verify(sidService, times(1)).lookupSnr(anyList(), eq(Optional.ofNullable(null)));
            Assertions.assertEquals("11854898347", mappedSid);
        }
    }

    @Test
    public void testMapValidVersion() {
        when(sidService.lookupFnr(anyList(), any(Optional.class))).thenReturn(Publishers.just(
                Maps.of("11854898347", new SidInfo.SidInfoBuilder().snr("0001ha3").build()))
        );
        when(sidService.getSnapshots()).thenReturn(Publishers.just(
                SnapshotInfo.builder().items(List.of("2023-04-25")).build()
        ));
        try (var application = mockStatic(Application.class)) {
            application.when(Application::getContext).thenReturn(context);
            Mapper mapper = ServiceLoader.load(Mapper.class).findFirst().orElseThrow(() ->
                    new RuntimeException("SidMapper class not found"));
            mapper.setConfig(Map.of("snapshotDate", "2023-04-25"));
            mapper.init(PseudoFuncInput.of("11854898347"));
            Object mappedSid = mapper.map(PseudoFuncInput.of("11854898347")).getFirstValue();
            verify(sidService, times(1)).getSnapshots();
            verify(sidService, times(1)).lookupFnr(anyList(), eq(Optional.of("2023-04-25")));
            Assertions.assertEquals("0001ha3", mappedSid);
        }
    }
    @Test
    public void testMapVersionEarlierThanAvailable() {
        when(sidService.getSnapshots()).thenReturn(Publishers.just(
                SnapshotInfo.builder().items(List.of("2023-04-25")).build()
        ));
        try (var application = mockStatic(Application.class)) {
            application.when(Application::getContext).thenReturn(context);
            Mapper mapper = ServiceLoader.load(Mapper.class).findFirst().orElseThrow(() ->
                    new RuntimeException("SidMapper class not found"));
            Exception exception = Assert.assertThrows(RuntimeException.class, () -> {
                mapper.setConfig(Map.of("snapshotDate", "2005-02-27"));
            });
            Assertions.assertEquals("Requested date is of an earlier date than all available SID dates. Valid dates are: 2023-04-25", exception.getMessage());
        }
    }

    @Test
    public void testMapVersionInvalidFormat() {
        when(sidService.getSnapshots()).thenReturn(Publishers.just(
                SnapshotInfo.builder().items(List.of("2023-04-25")).build()
        ));
        try (var application = mockStatic(Application.class)) {
            application.when(Application::getContext).thenReturn(context);
            Mapper mapper = ServiceLoader.load(Mapper.class).findFirst().orElseThrow(() ->
                    new RuntimeException("SidMapper class not found"));
            Exception exception = Assert.assertThrows(RuntimeException.class, () -> {
                mapper.setConfig(Map.of("snapshotDate", "25-04-2023"));
            });
            Assertions.assertEquals("Invalid snapshot date format. Valid dates are: 2023-04-25", exception.getMessage());
        }
    }
    @MockBean(SidService.class)
    SidService sidService() {
        return mock(SidService.class);
    }
}
