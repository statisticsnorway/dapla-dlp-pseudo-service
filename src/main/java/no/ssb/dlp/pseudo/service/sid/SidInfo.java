package no.ssb.dlp.pseudo.service.sid;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class SidInfo {
    private final String fnr;
    private final String fnr_naa;
    private final String snr;
}
