package no.ssb.dlp.pseudo.service.pseudo;

import lombok.Data;
import no.ssb.dlp.pseudo.core.func.PseudoFuncRule;
import no.ssb.dlp.pseudo.core.tink.model.EncryptedKeysetWrapper;

import java.util.ArrayList;
import java.util.List;

@Data
public class PseudoConfig {
    private List<PseudoFuncRule> rules = new ArrayList<>();
    private List<EncryptedKeysetWrapper> keysets = new ArrayList<>();
}
