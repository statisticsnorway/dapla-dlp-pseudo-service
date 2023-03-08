package no.ssb.dlp.pseudo.service.sid;

import com.univocity.parsers.annotations.Format;
import com.univocity.parsers.annotations.Parsed;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class SidItem implements Serializable {

    private final static String YYYYMMDD_DATE_FORMAT = "yyyyMMdd";
    private final static String NULL_DATE_FORMAT = ".";

    /**
     * Fødselsnr/Dnr, alle som har eksistert
     */
    @Parsed(field = SidMappingFileField.Name.FNR)
    private String fnr;

    /**
     * Nåværende fnr/Dnr
     */
    @Parsed(field = SidMappingFileField.Name.CURRENT_FNR)
    private String currentFnr;

    /**
     * SNR, alle som har eksistert
     */
    @Parsed(field = SidMappingFileField.Name.SNR)
    private String snr;

    /**
     * Nåværende Snr
     */
    @Parsed(field = SidMappingFileField.Name.CURRENT_SNR)
    private String currentSnr;

    /**
     * Dato for fnr/Dnr
     */
    @Parsed(field = SidMappingFileField.Name.FNR_DATE)
    @Format(formats = {YYYYMMDD_DATE_FORMAT, NULL_DATE_FORMAT})
    private Date fnrDate;

    /**
     * Dato for nåværende fnr/Dnr
     */
    @Parsed(field = SidMappingFileField.Name.CURRENT_FNR_DATE)
    @Format(formats = {YYYYMMDD_DATE_FORMAT, NULL_DATE_FORMAT})
    private Date currentFnrDate;

    /**
     * Dato for Snr
     */
    @Parsed(field = SidMappingFileField.Name.SNR_DATE)
    @Format(formats = {YYYYMMDD_DATE_FORMAT, NULL_DATE_FORMAT})
    private Date snrDate;

    /**
     * Registreringsdato for Snr
     */
    @Parsed(field = SidMappingFileField.Name.SNR_REGISTRATION_DATE)
    @Format(formats = {YYYYMMDD_DATE_FORMAT, NULL_DATE_FORMAT})
    private Date snrRegistrationDate;

    /**
     * Kjønn (1=mann, 2=kvinne)
     */
    @Parsed(field = SidMappingFileField.Name.GENDER)
    private String gender;

    /**
     * Fødselsdato
     */
    @Parsed(field = SidMappingFileField.Name.BIRTH_DATE)
    @Format(formats = {YYYYMMDD_DATE_FORMAT, NULL_DATE_FORMAT})
    private Date birthDate;

}
