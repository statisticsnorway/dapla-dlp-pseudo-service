package no.ssb.dlp.pseudo.service.pseudo.metadata;

public enum FieldMetric {
    // Signals that a pseudonymization function has encountered a null value
    NULL_VALUE,
    // Signals that a FPE pseudonymization function has failed because the value is less than or equal to 2 characters long
    FPE_LIMITATION,
    // Signals that a SID mapping has failed
    MISSING_SID,
    // Signals that a SID mapping has been performed
    MAPPED_SID;
}
