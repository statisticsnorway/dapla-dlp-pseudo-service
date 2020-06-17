package no.ssb.dlp.pseudo.service;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RecordMapPseudonymizer {
    private final FieldPseudonymizer fieldPseudonymizer;

    public RecordMap pseudonymize(RecordMap record) {
        return MapTraverser.traverse(record, fieldPseudonymizer::pseudonymize);
    }

    public RecordMap depseudonymize(RecordMap record) {
        return MapTraverser.traverse(record, fieldPseudonymizer::depseudonymize);
    }

}
