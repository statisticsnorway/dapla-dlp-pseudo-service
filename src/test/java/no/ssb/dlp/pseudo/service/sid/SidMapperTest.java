package no.ssb.dlp.pseudo.service.sid;

import io.micronaut.context.ApplicationContext;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import no.ssb.dapla.dlp.pseudo.func.map.Mapper;
import no.ssb.dlp.pseudo.service.Application;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
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
        when(sidService.lookupFnr(anyString(), any(Optional.class))).thenReturn(Publishers.just(
                new SidInfo.SidInfoBuilder().fnr("0001ha3").build())
        );
        // Use static mocking to override the application context
        try (var application = mockStatic(Application.class)) {
            application.when(Application::getContext).thenReturn(context);
            Mapper mapper = ServiceLoader.load(Mapper.class).findFirst().orElseThrow(() ->
                    new RuntimeException("SidMapper class not found"));
            Object mappedSid = mapper.map("11854898347");
            verify(sidService, times(1)).lookupFnr(anyString(), any(Optional.class));
            Assertions.assertEquals("0001ha3", mappedSid);
        }
    }

    @MockBean(SidService.class)
    SidService sidService() {
        return mock(SidService.class);
    }
}
