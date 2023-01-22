package no.ssb.dlp.pseudo.service.sid;

import com.google.auto.service.AutoService;
import com.google.common.base.Strings;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import no.ssb.dapla.dlp.pseudo.func.map.Mapper;
import no.ssb.dlp.pseudo.service.Application;

import java.util.Optional;

@AutoService(Mapper.class)
@Slf4j
public class SidMapper implements Mapper {

    private final SidCache sidCache;

    public SidMapper() {
        sidCache = Application.getContext().getBean(SidCache.class);
    }

    @Override
    public Object map(@NonNull Object data) {
        if (data == null) {
            return null;
        }

        String fnr = String.valueOf(data);
        Optional<String> snr = sidCache.getCurrentSnrForFnr(fnr);
        if (snr.isEmpty()) {
            log.warn("No SID-mapping found for fnr starting with " + Strings.padEnd(fnr, 6, ' ').substring(0,6));
            return data;
        }

        return snr.get();
    }

    @Override
    public Object restore(Object mapped) {
        return mapped;
    }
}
