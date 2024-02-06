package no.ssb.dlp.pseudo.service.pseudo.metadata;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder
public class FieldMetadata {

    String path;
    String name;
    String pattern;
    String func;
    String algorithm;
    Map<String, String> metadata;
    List<String> warnings;
}