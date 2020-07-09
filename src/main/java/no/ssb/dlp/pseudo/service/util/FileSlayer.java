package no.ssb.dlp.pseudo.service.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

@Slf4j
@UtilityClass
public class FileSlayer {

    public static void delete(Path path) {
        delete(path, true);
    }

    public static void delete(File file) {
        if (file != null) {
            delete(file.toPath(), true);
        }
    }

    public static void delete(Iterable<File> files) {
        for (File f : files) {
            delete(f);
        }
    }

    public static void deleteSilently(Path path) {
        delete(path, false);
    }

    public static void deleteSilently(File file) {
        if (file != null) {
            deleteSilently(file.toPath());
        }
    }

    public static void deleteSilently(Iterable<File> files) {
        for (File f : files) {
            deleteSilently(f);
        }
    }

    public static void deleteRecursively(Path path) {
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder())
              .forEach(FileSlayer::delete);
        }
        catch (Exception e) {
            throw new FileSlayerException("Error deleting file recursively from: " + path , e);
        }
    }

    public static void deleteRecursively(File file) {
        if (file != null) {
            deleteRecursively(file.toPath());
        }
    }

    public static void deleteRecursivelyAndSilently(Path path) {
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder())
              .forEach(FileSlayer::delete);
        }
        catch (Exception e) {
            log.warn("Error deleting file recursively and silently from: " + path , e);
        }
    }

    public static void deleteRecursivelyAndSilently(File file) {
        if (file != null) {
            deleteRecursivelyAndSilently(file.toPath());
        }
    }

    private static void delete(Path path, boolean throwExceptionOnError) {
        try {
            if (path != null) {
                Files.deleteIfExists(path);
            }
        }
        catch (Exception e) {
            if (throwExceptionOnError) {
                throw new FileSlayerException("Error deleting file: " + path, e);
            }
            else {
                log.warn("Ignored error deleting file: " + path , e);
            }
        }
    }

    public static class FileSlayerException extends RuntimeException {
        public FileSlayerException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}
