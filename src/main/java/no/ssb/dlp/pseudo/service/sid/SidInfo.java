package no.ssb.dlp.pseudo.service.sid;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class SidInfo {
    private final String currentSnr;
    private final String currentFnr;
}
