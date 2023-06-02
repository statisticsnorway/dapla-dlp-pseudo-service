package no.ssb.dlp.pseudo.service.sid;

import com.google.auto.service.AutoService;
import com.google.common.base.Strings;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import no.ssb.dapla.dlp.pseudo.func.map.Mapper;
import no.ssb.dlp.pseudo.service.Application;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
        ObservableSubscriber<SidInfo> subscriber = new ObservableSubscriber<>();
        sidService.lookupFnr(fnr, Optional.ofNullable(null)).subscribe(subscriber);
        SidInfo result = subscriber.awaitResult();
        if (result == null) {
            log.warn("No SID-mapping found for fnr starting with {}", Strings.padEnd(fnr, 6, ' ').substring(0, 6));
            return fnr;
        } else {
            log.debug("Successfully mapped fnr starting with {}", Strings.padEnd(fnr, 6, ' ').substring(0, 6));
            return result.getCurrentSnr();
        }
    }

    @Override
    public Object restore(Object mapped) {
        return mapped;
    }

    /**
     * A Subscriber that stores the publishers results and provides a latch so can block on completion.
     *
     * @param <T> The publishers result type
     */
    class ObservableSubscriber<T> implements Subscriber<T> {
        private final CountDownLatch latch = new CountDownLatch(1);
        private volatile T result;
        private volatile Subscription subscription;

        @Override
        public void onSubscribe(Subscription subscription) {
            this.subscription = subscription;
        }

        @Override
        public void onNext(T sidInfo) {
            result = sidInfo;
        }

        @Override
        public void onError(Throwable throwable) {
            HttpClientResponseException exception = (HttpClientResponseException) throwable;
            if (exception.getStatus() == HttpStatus.NOT_FOUND) {
                // This may happen more frequently, so log at debug level
                log.debug("Error was", exception);
            } else {
                log.warn("Unexpected error", exception);
            }
            onComplete();
        }

        @Override
        public void onComplete() {
            latch.countDown();
        }

        public T awaitResult() {
            return await().result;
        }

        private ObservableSubscriber<T> await() {
            subscription.request(1);
            try {
                if (!latch.await(Long.MAX_VALUE, TimeUnit.MILLISECONDS)) {
                    throw new RuntimeException("Publisher onComplete timed out");
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return this;
        }
    }
}