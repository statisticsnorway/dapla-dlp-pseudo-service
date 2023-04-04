package no.ssb.dlp.pseudo.service.sid.local;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Fields defined in the SID mapping file provided by Team FREG.
 *
 * !!! Enum ordering is important - must correspond to the field ordering of the source mapping file !!!
 */
@RequiredArgsConstructor
@Getter
enum SidMappingFileField {
    FNR(Name.FNR, 11),
    CURRENT_FNR(Name.CURRENT_FNR, 11),
    SNR(Name.SNR, 7),
    CURRENT_SNR(Name.CURRENT_SNR, 7),
    FNR_DATE(Name.FNR_DATE, 8),
    CURRENT_FNR_DATE(Name.CURRENT_FNR_DATE, 8),
    SNR_DATE(Name.SNR_DATE, 8),
    SNR_REGISTRATION_DATE(Name.SNR_REGISTRATION_DATE, 8),
    GENDER(Name.GENDER, 1),
    BIRTH_DATE(Name.BIRTH_DATE, 8),
    ;

    private final String originalName;
    private final int length;

    // Defining String constants for names is a bit awkward, but necessary since they are also addressed by annotations
    // in e.g. SidItem.
    static class Name {
        public static final String FNR = "fnr";
        public static final String CURRENT_FNR = "fnr_naa";
        public static final String SNR = "snr_utgatt";
        public static final String CURRENT_SNR = "snr";
        public static final String FNR_DATE = "dato_fnr";
        public static final String CURRENT_FNR_DATE = "dato_fnrnaa";
        public static final String SNR_DATE = "dato_snr";
        public static final String SNR_REGISTRATION_DATE = "rdato_snr";
        public static final String GENDER = "kjoenn";
        public static final String BIRTH_DATE = "fdato";
    }

}
