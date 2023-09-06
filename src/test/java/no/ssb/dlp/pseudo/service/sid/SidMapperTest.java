package no.ssb.dlp.pseudo.service.sid;

import io.micronaut.context.ApplicationContext;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import no.ssb.dapla.dlp.pseudo.func.map.Mapper;
import no.ssb.dlp.pseudo.service.Application;
import org.apache.groovy.util.Maps;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.swing.text.html.Option;
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
            mapper.init("11854898347");
            Object mappedSid = mapper.map("11854898347");
            verify(sidService, times(1)).lookupFnr(anyList(), eq(Optional.ofNullable(null)));
            Assertions.assertEquals("0001ha3", mappedSid);
        }
    }

    @Test
    public void testMapValidVersion() {
        when(sidService.lookupFnr(anyList(), any(Optional.class))).thenReturn(Publishers.just(
                Maps.of("11854898347", new SidInfo.SidInfoBuilder().snr("0001ha3").build()))
        );
        when(sidService.getSnapshots()).thenReturn(Publishers.just(
                VersionInfo.builder().items(List.of("12345")).build()
        ));
        try (var application = mockStatic(Application.class)) {
            application.when(Application::getContext).thenReturn(context);
            Mapper mapper = ServiceLoader.load(Mapper.class).findFirst().orElseThrow(() ->
                    new RuntimeException("SidMapper class not found"));
            mapper.setConfig(Map.of("versionTimestamp", "12345"));
            mapper.init("11854898347");
            Object mappedSid = mapper.map("11854898347");
            verify(sidService, times(1)).getSnapshots();
            verify(sidService, times(1)).lookupFnr(anyList(), eq(Optional.of("12345")));
            Assertions.assertEquals("0001ha3", mappedSid);
        }
    }
    @Test
    public void testMapInvalidVersion() {
        when(sidService.getSnapshots()).thenReturn(Publishers.just(
                VersionInfo.builder().items(List.of("12345")).build()
        ));
        try (var application = mockStatic(Application.class)) {
            application.when(Application::getContext).thenReturn(context);
            Mapper mapper = ServiceLoader.load(Mapper.class).findFirst().orElseThrow(() ->
                    new RuntimeException("SidMapper class not found"));
            Exception exception = Assert.assertThrows(RuntimeException.class, () -> {
                mapper.setConfig(Map.of("versionTimestamp", "1234567"));
            });
            Assertions.assertEquals("Invalid version timestamp. Valid versions are: 12345", exception.getMessage());
        }
    }

    @MockBean(SidService.class)
    SidService sidService() {
        return mock(SidService.class);
    }
}
