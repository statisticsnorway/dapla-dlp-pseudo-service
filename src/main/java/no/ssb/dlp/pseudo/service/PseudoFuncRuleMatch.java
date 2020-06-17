package no.ssb.dlp.pseudo.service;

import lombok.Value;
import no.ssb.dapla.dlp.pseudo.func.PseudoFunc;

@Value
class PseudoFuncRuleMatch {
    private final PseudoFunc func;
    private final PseudoFuncRule rule;
}
