package no.ssb.dlp.pseudo.service.sid;

import com.univocity.parsers.fixed.FixedWidthFields;
import com.univocity.parsers.fixed.FixedWidthParserSettings;
import com.univocity.parsers.fixed.FixedWidthRoutines;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.InputStream;

@RequiredArgsConstructor
@Singleton
@Slf4j
public class SidReader {

    private static final int FNR = 11;
    private static final int FNR_NAA = 11;
    private static final int SNR_UTGATT = 7;
    private static final int SNR = 7;
    private static final int DATO_FNR = 8;
    private static final int DATO_FNRNAA = 8;
    private static final int DATO_SNR = 8;
    private static final int RDATO_SNR = 8;
    private static final int KJOENN = 1;
    private static final int FDATO = 8;

    @SneakyThrows
    public void readSidsFromFile(String filePath, SidCache sidCache) {
        FileInputStream fis = new FileInputStream(filePath);
        readSidsFromFile(fis, sidCache);
    }

    public void readSidsFromFile(InputStream inputStream, SidCache sidCache) {
        FixedWidthFields fieldLengths = new FixedWidthFields(FNR, FNR_NAA, SNR_UTGATT, SNR, DATO_FNR, DATO_FNRNAA, DATO_SNR, RDATO_SNR, KJOENN, FDATO);
        FixedWidthParserSettings settings = new FixedWidthParserSettings(fieldLengths);
        settings.setHeaders("fnr", "fnr_naa", "snr_utgatt", "snr", "dato_fnr", "dato_fnrnaa", "dato_snr", "rdato_snr", "kjoenn", "fdato");

        FixedWidthRoutines routines = new FixedWidthRoutines(settings);
        for (SidItem sidItem : routines.iterate(SidItem.class, inputStream, "UTF-8")) {
            sidCache.register(sidItem);
        }
    }

}
