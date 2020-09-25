package no.ssb.dlp.pseudo.service;

import lombok.Data;

import java.util.List;

@Data
public class PseudoConfig {
    private List<PseudoFuncRule> rules;
}
