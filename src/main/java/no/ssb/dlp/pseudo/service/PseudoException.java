package no.ssb.dlp.pseudo.service;

public class PseudoException extends RuntimeException {
    public PseudoException(String message) {
        super(message);
    }

    public PseudoException(String message, Throwable cause) {
        super(message, cause);
    }
}
