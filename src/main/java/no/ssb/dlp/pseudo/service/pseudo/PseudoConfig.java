package no.ssb.dlp.pseudo.service.pseudo;

import lombok.Data;
import no.ssb.dlp.pseudo.core.PseudoFuncRule;

import java.util.ArrayList;
import java.util.List;

@Data
public class PseudoConfig {
    private List<PseudoFuncRule> rules = new ArrayList<>();
}
