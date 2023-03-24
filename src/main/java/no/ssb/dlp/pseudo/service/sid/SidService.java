package no.ssb.dlp.pseudo.service.sid;


import org.reactivestreams.Publisher;

public interface SidService {

    Publisher<SidInfo> lookupFnr(String fnr);

    Publisher<SidInfo> lookupSnr(String snr);

}
