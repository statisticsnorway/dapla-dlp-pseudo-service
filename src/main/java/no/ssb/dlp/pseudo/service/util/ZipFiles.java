package no.ssb.dlp.pseudo.service.util;

import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@UtilityClass
public class ZipFiles {

    public static List<File> unzip(File zippedFile, Path destPath) throws IOException {
        return unzip(zippedFile, destPath, false);
    }

    public static List<File> unzipAndDelete(File zippedFile, Path destPath) throws IOException {
        return unzip(zippedFile, destPath, true);
    }

    private static List<File> unzip(File zippedFile, Path destPath, boolean deleteZippedFile) throws IOException  {
        List<File> results = new ArrayList<>();
        byte[] buffer = new byte[1024];
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zippedFile))) {

            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File newFile = newFile(destPath, zipEntry);
                try (FileOutputStream fos = new FileOutputStream(newFile)) {
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
                results.add(newFile);
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
        }

        if (deleteZippedFile) {
            Files.deleteIfExists(zippedFile.toPath());
        }

        return results;
    }

    private static File newFile(Path destPath, ZipEntry zipEntry) throws IOException {
        Path filePath = Files.createFile(destPath.resolve(zipEntry.getName()));

        // Guard against zip slip (https://snyk.io/research/zip-slip-vulnerability)
        if (!filePath.startsWith(destPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return filePath.toFile();
    }

}
