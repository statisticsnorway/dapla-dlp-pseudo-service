package no.ssb.dlp.pseudo.service.secrets;

import lombok.Value;
import no.ssb.dlp.pseudo.service.PseudoException;

import java.util.Base64;

@Value
public class Secret {
    private final String type;
    private final byte[] content;

    public Secret(String base64EncodedContent, String type) {
        try {
            this.content = Base64.getDecoder().decode(base64EncodedContent);
        }
        catch (IllegalArgumentException e) {
            throw new PseudoException("Invalid secret. Must be a base64 encoded string");
        }
        this.type = type;
    }

    public String getBase64EncodedContent() {
        return Base64.getEncoder().encodeToString(content);
    }

}
