package no.ssb.dlp.pseudo.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.io.Files;
import io.micronaut.http.MediaType;
import lombok.extern.slf4j.Slf4j;
import no.ssb.dlp.pseudo.service.util.FileSizes;
import no.ssb.dlp.pseudo.service.util.FileTypes;
import no.ssb.dlp.pseudo.service.util.ZipFiles;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class PseudoFileSource {
    private final MediaType providedMediaType;
    private final MediaType mediaType;
    private final InputStream inputStream;
    private final List<File> allFiles;
    private final Collection<File> sourceFiles;

    public PseudoFileSource(File file) {
        this(file, null);
    }

    public PseudoFileSource(File file, MediaType sourceMediaType) {
        try {
            providedMediaType = FileTypes.determineFileType(file).orElse(null);
            allFiles = decompress(file, providedMediaType);
            Multimap<MediaType, File> filesByMediaType = filesByMediaType(allFiles);
            mediaType = (sourceMediaType == null) ? deduceMediaType(filesByMediaType) : sourceMediaType;
            sourceFiles = filesByMediaType.get(mediaType);
            if (sourceFiles.isEmpty()) {
                throw new PseudoException("No files of type " + mediaType + " found");
            }
            inputStream = concatenateFiles(sourceFiles);
        }
        catch (IOException e) {
            throw new PseudoException("Error initializing PseudoFileStream from file " + file, e);
        }
    }

    /**
     * @return the media type of the originally provided file, e.g. application/zip
     */
    public MediaType getProvidedMediaType() {
        return providedMediaType;
    }

    /**
     * @return the media type of source the files, e.g. application/json. This will be the same as "providedMediaType" if the
     * provided file was not an archive
     */
    public MediaType getMediaType() {
        return mediaType;
    }

    /**
     * If the provided file is a zip archive, this method returns a concatenated input stream
     * based on all files in the archive. If the provided file is not an archive, then the input
     * stream is sourced directly from the provided file.
     *
     * @return a (possibly concatenated) input stream for the provided the files
     */
    public InputStream getInputStream() {
        return inputStream;
    }

    /**
     * @return the files to (de)pseudonymize
     */
    public Collection<File> getFiles() {
        return sourceFiles;
    }

    /**
     * Cleanup action that can be invoked when PseudoFileSource has been processed to
     * explicitly delete the source files.
     */
    public void cleanup() {
        for (File f : allFiles) {
            if (f.exists()) {
                Preconditions.checkState(f.delete(), "Unable to delete file " + f.getName() + " during cleanup");
            }
        }
    }

    /**
     * Handle file decompression, if applicable
     */
    private static List<File> decompress(File file, MediaType mediaType) throws IOException {
        if (MoreMediaTypes.APPLICATION_ZIP_TYPE.equals(mediaType)) {
            log.info("Decompressing zip file (size={})...", FileSizes.humanReadableByteCountBin(file.length()));
            Path destPath = java.nio.file.Files.createTempDirectory("temp");
            List<File> files = ZipFiles.unzipAndDelete(file, destPath);
            StringBuilder sb = new StringBuilder();
            for (File f : files) {
                sb.append("- " + f.getName() + " ( " + FileSizes.humanReadableByteCountBin(f.length()) + ")\n");
            }
            log.info(files.size() == 0 ? "No files in archive..." : "Files in archive:\n" + sb.toString());
            return files;
        }

        return List.of(file);
    }

    /**
     * @return a Multimap of files ordered by MediaType. Files with unknown media type are excluded.
     */
    private static Multimap<MediaType, File> filesByMediaType(Collection<File> files) {
        return files.stream()
          .filter(f -> FileTypes.determineFileType(f).isPresent())
          .collect(Multimaps.toMultimap(
            f -> FileTypes.determineFileType(f).get(),
            Function.identity(),
            ArrayListMultimap::create
          ));
    }

    private static MediaType deduceMediaType(Multimap<MediaType, File> filesByMediaType) {
        if (filesByMediaType.keySet().size() == 0) {
            throw new PseudoException("No files with supported file types found.");
        }
        else if (filesByMediaType.keySet().size() > 1) {
            throw new PseudoException("Multiple file types encountered. Make sure to use the same file types on all files in archive.");
        }
        else {
            return filesByMediaType.keySet().stream().findFirst().get();
        }
    }

    private static InputStream concatenateFiles(Collection<File> files) {
        return new SequenceInputStream(
          Collections.enumeration(files.stream()
            .map(f -> {
                try {
                    return Files.asByteSource(f).openBufferedStream();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            })
            .collect(Collectors.toList()))
        );
    }

}
