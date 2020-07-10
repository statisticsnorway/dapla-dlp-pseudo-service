package no.ssb.dlp.pseudo.service.util;

import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import net.lingala.zip4j.ZipFile;
import no.ssb.dlp.pseudo.service.mediatype.CompressionEncryptionMethod;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static no.ssb.dlp.pseudo.service.util.FileSlayer.deleteSilently;
import static no.ssb.dlp.pseudo.service.util.FileUtils.readFileFromClasspath;
import static no.ssb.dlp.pseudo.service.util.Zips.ZipOptions.zipOpts;
import static org.assertj.core.api.Assertions.assertThat;

class ZipsTest {

    private static final char[] PASSWORD = "kensentme".toCharArray();

    private static void assertValidZip(Path p) {
        assertThat(new ZipFile(p.toFile()).isValidZipFile()).isTrue();
    }

    private static void assertInvalidZip(Path p) {
        assertThat(new ZipFile(p.toFile()).isValidZipFile()).isFalse();
    }

    private static void assertEncryptedZip(Path p) throws IOException {
        assertThat(new ZipFile(p.toFile()).isEncrypted()).isTrue();
    }

    @Test
    public void jsonFile_zip_shouldCreateZippedFile() throws IOException {
        Path pathToZip = Files.createTempDirectory("tst").resolve("test.zip");
        File content = readFileFromClasspath("data/somedata.json");
        assertInvalidZip(pathToZip);
        Zips.zip(pathToZip, content);

        assertValidZip(pathToZip);
        FileSlayer.deleteSilently(pathToZip);
    }

    @Test
    public void jsonFile_zip_shouldCreateZippedAndEncryptedFile() throws IOException {
        Path pathToZip = Files.createTempDirectory("tst").resolve("test.zip");
        File content = readFileFromClasspath("data/somedata.json");
        assertInvalidZip(pathToZip);
        Zips.zip(pathToZip, content, zipOpts()
          .encryptionMethod(CompressionEncryptionMethod.AES)
          .password(PASSWORD)
          .build());

        assertValidZip(pathToZip);
        assertEncryptedZip(pathToZip);
        FileSlayer.deleteSilently(pathToZip);
    }

    @Test
    public void flowable_zipFlowable_shouldCreateZippedFile() throws Exception {
        Path pathToZip = Files.createTempDirectory("test").resolve("test.zip");
        AtomicInteger calls = new AtomicInteger();
        Flowable<String> f = Flowable.range(100, 10).map(Object::toString)
          .doOnCancel(() -> calls.incrementAndGet())
          .subscribeOn(Schedulers.computation())
          .delay(10, TimeUnit.MILLISECONDS);
        Zips.zip(pathToZip, f, "something.txt");

        assertValidZip(pathToZip);
        FileSlayer.deleteSilently(pathToZip);
    }

    @Test
    public void flowable_zipFlowable_shouldCreateZippedAndEncryptedFile() throws Exception {
        Path pathToZip = Files.createTempDirectory("test").resolve("test.zip");
        AtomicInteger calls = new AtomicInteger();
        Flowable<String> f = Flowable.range(100, 10).map(Object::toString)
          .doOnCancel(() -> calls.incrementAndGet())
          .subscribeOn(Schedulers.computation())
          .delay(10, TimeUnit.MILLISECONDS);
        Zips.zip(pathToZip, f, "something.txt", zipOpts()
          .encryptionMethod(CompressionEncryptionMethod.AES)
          .password(PASSWORD)
          .build());

        assertValidZip(pathToZip);
        assertEncryptedZip(pathToZip);
        FileSlayer.deleteSilently(pathToZip);
    }

    @Test
    public void flowable_zipFlowable_shouldCreateZippedFlowable() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        Flowable<String> f = Flowable.range(100, 10).map(Object::toString)
          .doOnCancel(() -> calls.incrementAndGet())
          .subscribeOn(Schedulers.computation())
          .delay(10, TimeUnit.MILLISECONDS);
        Flowable<byte[]> flowableZip = Zips.zip(f, "somecontent.txt");

        final Path pathToZip = Files.createTempDirectory("test").resolve(UUID.randomUUID() + ".zip");
        pathToZip.toFile().createNewFile();
        assertInvalidZip(pathToZip);

        flowableZip.subscribe(
          (byte[] bytes) -> {
              Files.write(pathToZip, bytes, StandardOpenOption.APPEND);
          });

        assertValidZip(pathToZip);
        FileSlayer.deleteSilently(pathToZip);
    }

    @Test
    public void zipFileWithSingleEntry_unzip_shouldReturnUnzippedFile() throws IOException {
        File zipFile = readFileFromClasspath("data/single-json-file.zip");
        Path destPath = Files.createTempDirectory("temp");
        Set<File> files = Zips.unzip(zipFile, destPath);

        assertThat(files.size()).isEqualTo(1);
        FileSlayer.deleteSilently(files);
    }

    @Test
    public void zipFileWithMultipleEntries_unzip_shouldReturnUnzippedFiles() throws IOException {
        File zipFile = readFileFromClasspath("data/multiple-json-files.zip");
        Path destPath = Files.createTempDirectory("temp");
        Set<File> files = Zips.unzip(zipFile, destPath);

        assertThat(files.size()).isEqualTo(10);
        deleteSilently(files);
    }


}