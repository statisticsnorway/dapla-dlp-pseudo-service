package no.ssb.dlp.pseudo.service.export;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

@Data
@ConfigurationProperties("export")
public class ExportConfig {

    /**
     * The default root path of exports. E.g. gs://[export-bucket-name]/export
     */
    private String defaultTargetRoot;

}
