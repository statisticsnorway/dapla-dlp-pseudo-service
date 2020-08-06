package no.ssb.dlp.pseudo.service;

import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Part;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.multipart.StreamingFileUpload;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.reactivex.Flowable;
import io.reactivex.Single;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.ssb.dlp.pseudo.service.security.PseudoServiceRole;
import no.ssb.dlp.pseudo.service.mediatype.CompressionEncryptionMethod;
import no.ssb.dlp.pseudo.service.mediatype.MoreMediaTypes;
import no.ssb.dlp.pseudo.service.util.HumanReadableBytes;
import no.ssb.dlp.pseudo.service.util.Json;
import no.ssb.dlp.pseudo.service.util.Zips;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static no.ssb.dlp.pseudo.service.util.Zips.ZipOptions.zipOpts;

// TODO: Rename to PseudoFileController?

@RequiredArgsConstructor
@Controller
@Slf4j
@Secured(SecurityRule.IS_AUTHENTICATED)
public class PseudoController {

    private final PseudonymizerFactory pseudonymizerFactory;

    @Post("/pseudonymize/file")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces({MediaType.APPLICATION_JSON, MoreMediaTypes.TEXT_CSV})
    public HttpResponse<Flowable> pseudonymizeFile(
              @Part("request") String requestString,
              StreamingFileUpload data,
              HttpHeaders headers) {
        PseudoRequest request = Json.toObject(PseudoRequest.class, requestString);
        try {
            ProcessFileResult res = processFile(request, data, headers, PseudoOperation.PSEUDONYMIZE);
            return HttpResponse.ok(res.getFlowable())
              .contentType(res.getTargetContentType());
        }
        catch (Exception e) {
            return HttpResponse.serverError(Flowable.error(e));
        }
   }

    @Post("/depseudonymize/file")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces({MediaType.APPLICATION_JSON, MoreMediaTypes.TEXT_CSV})
    @Secured({PseudoServiceRole.ADMIN})
    public HttpResponse<Flowable> depseudonymizeFile(
      @Part("request") String requestString,
      StreamingFileUpload data,
      HttpHeaders headers) {
        PseudoRequest request = Json.toObject(PseudoRequest.class, requestString);
        try {
            ProcessFileResult res = processFile(request, data, headers, PseudoOperation.DEPSEUDONYMIZE);
            return HttpResponse.ok(res.getFlowable())
              .contentType(res.getTargetContentType());
        }
        catch (Exception e) {
            return HttpResponse.serverError(Flowable.error(e));
        }
    }

    private ProcessFileResult processFile(PseudoRequest request, StreamingFileUpload data, HttpHeaders headers, PseudoOperation operation) throws IOException {
        MediaType targetContentType = targetContentType(headers.accept());
        File tempFile = null;
        PseudoFileSource fileSource = null;
        try {
            tempFile = receiveFile(data).blockingGet();
            fileSource = new PseudoFileSource(tempFile);
            log.info("Received file ({}, {})", fileSource.getProvidedMediaType(), HumanReadableBytes.fromBin(tempFile.length()));
            log.info("{} {} files with content type {}", operation, fileSource.getFiles().size(), fileSource.getMediaType());
            log.info("Target content type: {}", targetContentType);
            StreamPseudonymizer pseudonymizer = pseudonymizerFactory.newStreamPseudonymizer(request.getPseudoConfig().getRules(), fileSource.getMediaType());
            Flowable res = processStream(operation, fileSource.getInputStream(), targetContentType, pseudonymizer);
            Compression compression = request.getCompression();
            if (compression != null && compression.isZipCompressionEnabled()) {
                log.info("Applying target compression: " + request.getCompression().getType());
                String contentFilename = (operation + "-" + System.currentTimeMillis() + "." + targetContentType.getExtension()).toLowerCase();
                Flowable res2 = Zips.zip(res, contentFilename, zipOpts()
                    .password(compression.getPassword())
                    .encryptionMethod(compression.getEncryption())
                    .build()
                ); // TODO: Validate that this works
                return new ProcessFileResult(res2, targetContentType);
            }

            return new ProcessFileResult(res, targetContentType);
        }
        finally {
            try {
                if (fileSource != null) {
                    fileSource.cleanup();
                }
                if (tempFile != null) {
                    Files.deleteIfExists(tempFile.toPath());
                }
            }
            catch (IOException e) {
                log.warn("Error cleaning up", e);
            }
        }
    }

    private Flowable processStream(PseudoOperation operation, InputStream is, MediaType targetContentType, StreamPseudonymizer pseudonymizer) {
        return operation == PseudoOperation.PSEUDONYMIZE
          ? pseudonymizer.pseudonymize(is, RecordMapSerializerFactory.newFromMediaType(targetContentType))
          : pseudonymizer.depseudonymize(is, RecordMapSerializerFactory.newFromMediaType(targetContentType));
    }

    private MediaType targetContentType(List<MediaType> acceptedMediaTypes) {
        if (acceptedMediaTypes.contains(MoreMediaTypes.TEXT_CSV_TYPE)) {
            return MoreMediaTypes.TEXT_CSV_TYPE;
        }
        else if (acceptedMediaTypes.contains(MediaType.APPLICATION_JSON_TYPE)) {
            return MediaType.APPLICATION_JSON_TYPE;
        }
        else if (acceptedMediaTypes.contains(MediaType.ALL_TYPE)) {
            return MediaType.APPLICATION_JSON_TYPE; // Default to application/json
        }
        else {
            throw new IllegalArgumentException("Unsupported media type: " + acceptedMediaTypes);
        }
    }

    private Single<File> receiveFile(StreamingFileUpload data) throws IOException {
        Path tempDir = java.nio.file.Files.createTempDirectory("temp");
        File tempFile = tempDir.resolve(data.getFilename()).toFile();
        log.debug("Receive file - stored temporarily at " + tempFile.getAbsolutePath());
        return Single.fromPublisher(data.transferTo(tempFile))
          .map(success -> {
              if (Boolean.TRUE.equals(success)) {
                  return tempFile;
              }
              else {
                  throw new IOException("Error receiving file " + tempFile);
              }
          });
    }

    @Data
    static class PseudoRequest {
        private DatasetId sourceDataset;
        private DatasetId targetDataset;
        private PseudoConfig pseudoConfig;
        private Compression compression;

        public boolean isCompressionEnabled() {
            return compression != null && MoreMediaTypes.APPLICATION_ZIP_TYPE.equals(compression.getType());
        }
    }

    @Data
    @Builder
    static class PseudoResponse {
        private DatasetId targetDataset;
    }

    @Data
    static class PseudoConfig {
        private List<PseudoFuncRule> rules;
    }

    @Data
    static class DatasetId {
        private String path;
        private String version;
    }

    @Data
    static class ProcessFileResult {
        private final Flowable flowable;
        private final MediaType targetContentType;
    }

    @Data
    static class Compression {
        private MediaType type;
        private CompressionEncryptionMethod encryption;
        private char[] password;

        public boolean isZipCompressionEnabled() {
            return MoreMediaTypes.APPLICATION_ZIP_TYPE.equals(type);
        }

    }
}
