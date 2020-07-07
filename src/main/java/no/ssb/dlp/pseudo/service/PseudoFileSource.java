package no.ssb.dlp.pseudo.service;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import io.micronaut.http.MediaType;
import lombok.extern.slf4j.Slf4j;
import no.ssb.dlp.pseudo.service.mediatype.FileTypes;
import no.ssb.dlp.pseudo.service.mediatype.MoreMediaTypes;
import no.ssb.dlp.pseudo.service.util.HumanReadableBytes;
import no.ssb.dlp.pseudo.service.util.ZipFiles;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
            providedMediaType = FileTypes.probeContentType(file).orElse(null);
            allFiles = decompress(file, providedMediaType);
            Multimap<MediaType, File> filesByMediaType = filesByMediaType(allFiles);
            mediaType = Optional.ofNullable(sourceMediaType).orElse(deduceMediaType(filesByMediaType));
            sourceFiles = filesByMediaType.get(mediaType);
            if (sourceFiles.isEmpty()) {
                throw new PseudoException("No files of type " + mediaType + " found");
            }
            inputStream = inputStreamOf(sourceFiles);
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
    public void cleanup() throws IOException {
        for (File f : allFiles) {
            Files.deleteIfExists(f.toPath());
        }
    }

    /**
     * Handle file decompression, if applicable
     */
    private static List<File> decompress(File file, MediaType mediaType) throws IOException {
        if (MoreMediaTypes.APPLICATION_ZIP_TYPE.equals(mediaType)) {
            log.info("Decompressing zip file (size={})...", HumanReadableBytes.fromBin(file.length()));
            Path destPath = java.nio.file.Files.createTempDirectory("temp");
            List<File> files = ZipFiles.unzipAndDelete(file, destPath);
            StringBuilder sb = new StringBuilder();
            for (File f : files) {
                sb.append("- " + f.getName() + " ( " + HumanReadableBytes.fromBin(f.length()) + ")\n");
            }
            log.info(files.isEmpty() ? "No files in archive..." : "Files in archive:\n" + sb.toString());
            return files;
        }

        return List.of(file);
    }

    /**
     * @return a Multimap of files ordered by MediaType. Files with unknown media type are excluded.
     */
    private static Multimap<MediaType, File> filesByMediaType(Collection<File> files) {
        return files.stream()
          .filter(f -> {
              Optional<MediaType> fileType = FileTypes.probeContentType(f);
              if (! fileType.isPresent()) {
                  log.info("Unable to deduce file type, ignoring file: {}", f.getName());
              }
              return fileType.isPresent();
          })
          .collect(Multimaps.toMultimap(
            f -> FileTypes.probeContentType(f).get(),
            Function.identity(),
            ArrayListMultimap::create
          ));
    }

    private static MediaType deduceMediaType(Multimap<MediaType, File> filesByMediaType) {
        if (filesByMediaType.keySet().isEmpty()) {
            throw new PseudoException("No files with supported file types found.");
        }
        else if (filesByMediaType.keySet().size() > 1) {
            throw new PseudoException("Multiple file types encountered. Make sure to use the same file types on all files.");
        }
        else {
            return filesByMediaType.keySet().stream().findFirst().orElseThrow();
        }
    }

    private static InputStream inputStreamOf(Collection<File> files) {
        return new SequenceInputStream(
          Collections.enumeration(files.stream()
            .map(f -> {
                try {
                    return com.google.common.io.Files.asByteSource(f).openBufferedStream();
                } catch (IOException e) {
                    throw new PseudoException("Error concatenating file input streams", e);
                }
            })
            .collect(Collectors.toList()))
        );
    }

}
