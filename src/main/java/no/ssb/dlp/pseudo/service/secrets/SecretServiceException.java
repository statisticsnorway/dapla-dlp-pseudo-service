package no.ssb.dlp.pseudo.service.secrets;

public class SecretServiceException extends RuntimeException {
    public SecretServiceException(String message) {
        super(message);
    }

    public SecretServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
