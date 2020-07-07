package no.ssb.dlp.pseudo.service.util;

import com.google.common.base.Preconditions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static no.ssb.dlp.pseudo.service.util.FileUtils.readFileFromClasspath;
import static org.assertj.core.api.Assertions.assertThat;

class ZipFilesTest {

    void deleteFiles(Iterable<File> files) {
        for (File f : files) {
            Preconditions.checkState(f.delete(), "Failed to delete file " + f);
        }
    }

    @Test
    public void zipFileWithSingleEntry_unzip_shouldReturnUnzippedFile() throws IOException {
        File zipFile = readFileFromClasspath("data/single-json-file.zip");
        Path destPath = Files.createTempDirectory("temp");
        List<File> files = ZipFiles.unzip(zipFile, destPath);

        assertThat(files.size()).isEqualTo(1);
        deleteFiles(files);
    }

    @Test
    public void zipFileWithMultipleEntries_unzip_shouldReturnUnzippedFiles() throws IOException {
        File zipFile = readFileFromClasspath("data/multiple-json-files.zip");
        Path destPath = Files.createTempDirectory("temp");
        List<File> files = ZipFiles.unzip(zipFile, destPath);

        assertThat(files.size()).isEqualTo(10);
        deleteFiles(files);
    }
}