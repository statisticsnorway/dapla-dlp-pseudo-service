package no.ssb.dlp.pseudo.service.util;

import com.github.davidmoten.rx2.Bytes;
import com.github.davidmoten.rx2.Strings;
import io.reactivex.Flowable;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import no.ssb.dlp.pseudo.service.mediatype.CompressionEncryptionMethod;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static no.ssb.dlp.pseudo.service.util.FileSlayer.deleteSilently;

@UtilityClass
@Slf4j
public class Zips {

    public static void zip(Path pathToZip, File content) throws IOException {
        zip(pathToZip, content, ZipOptions.DEFAULT);
    }

    public static void zip(Path pathToZip, File content, ZipOptions options) throws IOException {
        new ZipFile(pathToZip.toFile(), options.getPassword()).addFile(content, options.toZip4jParameters());
    }

    public static void zip(Path pathToZip, Flowable<String> source, String contentFilename) {
        zip(pathToZip, source, contentFilename, ZipOptions.DEFAULT);
    }

    public static void zip(Path pathToZip, Flowable<String> source, String contentFilename, ZipOptions options) {
        ZipFile zipFile = new ZipFile(pathToZip.toFile(), options.getPassword());
        ZipParameters zipParams = options.toZip4jParameters();
        zipParams.setFileNameInZip(contentFilename);
        try {
            zipFile.addStream(Strings.toInputStream(source), zipParams);
        }
        catch (Exception e) {
            throw new ZipException("Error zipping Flowable with content=" + contentFilename + " to file " + pathToZip, e);
        }
    }

    public static Flowable<byte[]> zip(Flowable<String> source, String contentFilename) {
        return zip(source, contentFilename, ZipOptions.DEFAULT);
    }

    public static Flowable<byte[]> zip(Flowable<String> source, String contentFilename, ZipOptions options) {
        final Path tmpZipFile;
        try {
            String tmpName = UUID.randomUUID().toString();
            tmpZipFile = Files.createTempDirectory(tmpName).resolve(tmpName + ".zip");
            Zips.zip(tmpZipFile, source, contentFilename, options);
            return Bytes.from(tmpZipFile.toFile())
              .doAfterTerminate(() -> deleteSilently(tmpZipFile));
        }
        catch (Exception e) {
            throw new ZipException("Error zipping Flowable with content=" + contentFilename + " to Flowable", e);
        }
    }

    public static Set<File> unzip(File zippedFile, Path destPath) throws IOException {
        return unzip(zippedFile, destPath, UnzipOptions.DEFAULT);
    }

    public static Set<File> unzip(File zippedFile, Path destPath, UnzipOptions options) throws IOException {
        new ZipFile(zippedFile, options.getPassword())
          .extractAll(destPath.toAbsolutePath().toString());
        Set<File> files;
        try (Stream<Path> stream = Files.walk(destPath)) {
            files = stream
              .sorted(Comparator.naturalOrder())
              .filter(file -> !Files.isDirectory(file))
              .map(Path::toFile)
              .collect(Collectors.toCollection( LinkedHashSet::new ));
        }

        if (options.deleteAfterUnzip) {
            FileSlayer.delete(zippedFile);
        }

        return files;
    }

    public static class ZipException extends RuntimeException {
        public ZipException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    @Data
    @Builder
    public static class UnzipOptions {
        private boolean deleteAfterUnzip = false;
        private char[] password = null;

        static final UnzipOptions DEFAULT = UnzipOptions.builder()
          .deleteAfterUnzip(false)
          .build();

        public static UnzipOptions.UnzipOptionsBuilder unzipOpts() {
            return UnzipOptions.builder();
        }
    }

    @Data
    @Builder
    public static class ZipOptions {

        private char[] password;
        private CompressionEncryptionMethod encryptionMethod;

        static final ZipOptions DEFAULT = ZipOptions.builder().build();

        public static ZipOptions.ZipOptionsBuilder zipOpts() {
            return ZipOptions.builder();
        }

        ZipParameters toZip4jParameters() {
            ZipParameters params = new ZipParameters();
            params.setUnixMode(true);

            if (encryptionMethod != null) {
                params.setEncryptFiles(true);
                params.setEncryptionMethod(EncryptionMethod.valueOf(encryptionMethod.name()));
                params.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
            }

            return params;
        }
    }

}
