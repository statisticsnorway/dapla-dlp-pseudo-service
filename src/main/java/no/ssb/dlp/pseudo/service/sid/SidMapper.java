package no.ssb.dlp.pseudo.service.sid;

import com.google.auto.service.AutoService;
import com.google.common.base.Strings;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import no.ssb.dapla.dlp.pseudo.func.map.Mapper;
import no.ssb.dlp.pseudo.service.Application;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.Optional;

/**
 * Service Provider class that implements the {@link Mapper} pseudo function. This class will be invoked by the JDK's
 * Service Loader mechanism for {@link no.ssb.dlp.pseudo.core.func.PseudoFuncNames.MAP_SID} pseudo functions.
 */
@AutoService(Mapper.class)
@Slf4j
public class SidMapper implements Mapper {

    private final SidService sidService;

    public SidMapper() {
        sidService = Application.getContext().getBean(SidService.class);
    }

    @Override
    public Object map(@NonNull Object data) {
        if (data == null) {
            return null;
        }

        String fnr = String.valueOf(data);
        final Object[] result = new Object[1];
        sidService.lookupFnr(fnr, Optional.ofNullable(null)).subscribe(new Subscriber<>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.request(1);
            }

            @Override
            public void onNext(SidInfo sidInfo) {
                result[0] = sidInfo.getCurrentSnr();
            }

            @Override
            public void onError(Throwable throwable) {
                log.warn("No SID-mapping found for fnr starting with " + Strings.padEnd(fnr, 6, ' ').substring(0, 6));
                result[0] = data;
            }

            @Override
            public void onComplete() {
            }
        });
        return result[0];
    }

    @Override
    public Object restore(Object mapped) {
        return mapped;
    }
}
