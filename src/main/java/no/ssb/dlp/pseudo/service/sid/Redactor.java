package no.ssb.dlp.pseudo.service.sid;

import com.google.common.base.Strings;

/**
 * Redacts identifiers used by SID mapping.
 */
public class Redactor {

    static int MAX_VISIBLE_CHARS_FNR = 6;
    static int MAX_VISIBLE_CHARS_SNR = 4;

    private Redactor() {
    }

    public static String redactFnr(String fnr) {
        return redact(fnr, MAX_VISIBLE_CHARS_FNR);
    }

    public static String redactSnr(String snr) {
        return redact(snr, MAX_VISIBLE_CHARS_SNR);
    }

    public static String redact(String identifier, int maxVisibleChars) {
        if (identifier == null || identifier.length() <= maxVisibleChars) {
            return identifier;
        } else {
            return identifier.substring(0, maxVisibleChars) + Strings.repeat("*", identifier.length() - maxVisibleChars);
        }
    }
}
