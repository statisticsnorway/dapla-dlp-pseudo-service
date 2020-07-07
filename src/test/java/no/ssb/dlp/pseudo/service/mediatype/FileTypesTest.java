package no.ssb.dlp.pseudo.service.mediatype;

import io.micronaut.http.MediaType;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static no.ssb.dlp.pseudo.service.util.FileUtils.readFileFromClasspath;
import static org.assertj.core.api.Assertions.assertThat;

class FileTypesTest {

    @Test
    public void zipFile_determineFileType_shouldIdentifyFileType() {
        Optional<MediaType> fileType = FileTypes.probeContentType(readFileFromClasspath( "data/single-json-file.zip"));
        assertThat(fileType.isPresent()).isTrue();
        assertThat(MoreMediaTypes.APPLICATION_ZIP_TYPE).isEqualTo(fileType.get());
    }

    @Test
    public void jsonFile_determineFileType_shouldIdentifyFileType() {
         Optional<MediaType> fileType = FileTypes.probeContentType(readFileFromClasspath("data/somedata.json"));
        assertThat(fileType.isPresent()).isTrue();
        assertThat(MediaType.APPLICATION_JSON_TYPE).isEqualTo(fileType.get());
    }

    @Test
    public void csvFile_determineFileType_shouldIdentifyFileType() {
        Optional<MediaType> fileType = FileTypes.probeContentType(readFileFromClasspath("data/somedata.csv"));
        assertThat(fileType.isPresent()).isTrue();
        assertThat(MoreMediaTypes.TEXT_CSV_TYPE).isEqualTo(fileType.get());
    }

}