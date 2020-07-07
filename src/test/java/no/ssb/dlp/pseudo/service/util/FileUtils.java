package no.ssb.dlp.pseudo.service.util;

import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@UtilityClass
public class FileUtils {

    public static File readFileFromClasspath(String filename) {
        return new File(FileUtils.class.getClassLoader().getResource(filename).getFile());
    }

    public static File readFileFromClasspathAndCreateLocalCopy(String filename) throws IOException {
        File f = readFileFromClasspath(filename);
        Path localCopy = Files.createTempDirectory("testtemp").resolve(f.getName());
        Files.copy(f.toPath(), localCopy, StandardCopyOption.REPLACE_EXISTING);
        return localCopy.toFile();
    }

}
