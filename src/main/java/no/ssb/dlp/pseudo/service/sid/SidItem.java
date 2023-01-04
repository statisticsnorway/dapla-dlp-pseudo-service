package no.ssb.dlp.pseudo.service.sid;

import com.univocity.parsers.annotations.Parsed;
import lombok.Data;

@Data
public class SidItem {

    /**
     * Fødselsnr/Dnr, alle som har eksistert
     */
    @Parsed(field = "fnr")
    private String fnr;

    /**
     * Nåværende fnr/Dnr
     */
    @Parsed(field = "fnr_naa")
    private String fnrCurrent;

    /**
     * SNR, alle som har eksistert
     */
    @Parsed(field = "snr_utgatt")
    private String snr;

    /**
     * Nåværende Snr
     */
    @Parsed(field = "snr")
    private String currentSnr;

    /**
     * Dato for fnr/Dnr
     */
    @Parsed(field = "dato_fnr")
    private String fnrDate;

    /**
     * Dato for nåværende fnr/Dnr
     */
    @Parsed(field = "dato_fnrnaa")
    private String  fnrCurrentDate;

    /**
     * Dato for Snr
     */
    @Parsed(field = "dato_snr")
    private String snrDate;

    /**
     * Registreringsdato for Snr
     */
    @Parsed(field = "rdato_snr")
    private String snrRegistrationDate;

    /**
     * Kjønn (1=mann, 2=kvinne)
     */
    @Parsed(field = "kjoenn")
    private String gender;

    /**
     * Fødselsdato
     */
    @Parsed(field = "fdato")
    private String birthDate;

}
