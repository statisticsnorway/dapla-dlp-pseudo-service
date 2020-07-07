package no.ssb.dlp.pseudo.service;

import io.micronaut.http.MediaType;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static no.ssb.dlp.pseudo.service.util.FileUtils.readFileFromClasspathAndCreateLocalCopy;
import static org.assertj.core.api.Assertions.assertThat;

class PseudoFileSourceTest {

    @Test
    public void zipFileWithMultipleEntries_newPseudoFileSource_shouldReturnCombinedInputStream() throws IOException {
        File file = readFileFromClasspathAndCreateLocalCopy("data/multiple-json-files.zip");
        PseudoFileSource pfs = new PseudoFileSource(file);
        assertThat(pfs.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);
        assertThat(pfs.getProvidedMediaType()).isEqualTo(MoreMediaTypes.APPLICATION_ZIP_TYPE);
        assertThat(pfs.getFiles().size()).isEqualTo(10);
        pfs.cleanup();
    }

    @Test
    public void zipFile_pseudoFileSourceCleanup_shouldRemoveFiles() throws IOException {
        File file = readFileFromClasspathAndCreateLocalCopy("data/multiple-json-files.zip");
        PseudoFileSource pfs = new PseudoFileSource(file);
        assertThat(pfs.getFiles().size()).isEqualTo(10);
        for (File f : pfs.getFiles()) {
            assertThat(f.exists()).isTrue();
        }
        pfs.cleanup();
        for (File f : pfs.getFiles()) {
            assertThat(f.exists()).isFalse();
        }
    }

}