package no.ssb.dlp.pseudo.service.sid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SidReaderTest {

    @Test
    void testReadSidsFromFile() {
        String filePath = "private/sid/staging/snr-kat-latest";
        SidCache sidCache = new SidCache();
        SidReader sidReader = new SidReader();
        sidReader.readSidsFromFile(filePath, sidCache);
    }

}