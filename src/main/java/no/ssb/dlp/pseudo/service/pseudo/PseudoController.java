package no.ssb.dlp.pseudo.service.pseudo;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.multipart.StreamingFileUpload;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.ssb.dapla.storage.client.backend.gcs.GoogleCloudStorageBackend;
import no.ssb.dlp.pseudo.core.PseudoOperation;
import no.ssb.dlp.pseudo.core.StreamPseudonymizer;
import no.ssb.dlp.pseudo.core.file.CompressionEncryptionMethod;
import no.ssb.dlp.pseudo.core.file.MoreMediaTypes;
import no.ssb.dlp.pseudo.core.file.PseudoFileSource;
import no.ssb.dlp.pseudo.core.map.RecordMapSerializerFactory;
import no.ssb.dlp.pseudo.core.util.HumanReadableBytes;
import no.ssb.dlp.pseudo.core.util.Json;
import no.ssb.dlp.pseudo.core.util.Zips;
import no.ssb.dlp.pseudo.service.security.PseudoServiceRole;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static no.ssb.dlp.pseudo.core.util.Zips.ZipOptions.zipOpts;

@RequiredArgsConstructor
@Controller
@Slf4j
@Secured(SecurityRule.IS_AUTHENTICATED)
public class PseudoController {

    private final PseudonymizerFactory pseudonymizerFactory;

    private final GoogleCloudStorageBackend storageBackend;

    @Post("/pseudonymize/file")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces({MediaType.APPLICATION_JSON, MoreMediaTypes.TEXT_CSV, MoreMediaTypes.APPLICATION_ZIP})
    public HttpResponse<Flowable> pseudonymizeFile(@Schema(implementation = PseudoRequest.class) String request, StreamingFileUpload data) {
        try {
            PseudoRequest pseudoRequest = Json.toObject(PseudoRequest.class, request);
            ProcessFileResult res = processFile(pseudoRequest, data, PseudoOperation.PSEUDONYMIZE);
            Flowable file = res.getFlowable();
            if (pseudoRequest.getTargetUri() != null) {
                URI targetUri = pseudoRequest.getTargetUri();
                Completable fileUpload = storageBackend
                        .write(targetUri.toString(), file)
                        .timeout(30, TimeUnit.SECONDS)
                        .doOnError(throwable -> log.error("Upload failed: %s".formatted(targetUri), throwable))
                        .doOnComplete(() -> log.info("Successful upload: %s".formatted(targetUri)));
                return HttpResponse.ok(fileUpload.toFlowable());
            }
            return HttpResponse.ok(file).contentType(res.getTargetContentType());
        } catch (Exception e) {
            log.error(String.format("Failed to pseudonymize:\nrequest:\n%s", request), e);
            return HttpResponse.serverError(Flowable.error(e));
        }
    }

    @Post("/depseudonymize/file")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces({MediaType.APPLICATION_JSON, MoreMediaTypes.TEXT_CSV, MediaType.APPLICATION_OCTET_STREAM})
    @Secured({PseudoServiceRole.ADMIN})
    public HttpResponse<Flowable> depseudonymizeFile(@Schema(implementation = PseudoRequest.class) String request, StreamingFileUpload data) {
        try {
            PseudoRequest pseudoRequest = Json.toObject(PseudoRequest.class, request);
            ProcessFileResult res = processFile(pseudoRequest, data, PseudoOperation.DEPSEUDONYMIZE);
            Flowable file = res.getFlowable();
            if (pseudoRequest.getTargetUri() != null) {
                URI targetUri = pseudoRequest.getTargetUri();
                Completable fileUpload = storageBackend
                        .write(targetUri.toString(), file)
                        .timeout(30, TimeUnit.SECONDS)
                        .doOnError(throwable -> log.error("Upload failed: %s".formatted(targetUri), throwable))
                        .doOnComplete(() -> log.info("Successful upload: %s".formatted(targetUri)));
                return HttpResponse.ok(fileUpload.toFlowable());
            }
            else {
                return HttpResponse.ok(file).contentType(res.getTargetContentType());
            }
        } catch (Exception e) {
            log.error(String.format("Failed to depseudonymize:\nrequest:\n%s", request), e);
            return HttpResponse.serverError(Flowable.error(e));
        }
    }

    private ProcessFileResult processFile(PseudoRequest request, StreamingFileUpload data, PseudoOperation operation) throws IOException {
        MediaType targetContentType = MoreMediaTypes.validContentType(request.getTargetContentType());
        File tempFile = null;
        PseudoFileSource fileSource = null;

        try {
            tempFile = receiveFile(data).blockingGet();
            fileSource = new PseudoFileSource(tempFile);
            log.info("Received file ({}, {})", fileSource.getProvidedMediaType(), HumanReadableBytes.fromBin(tempFile.length()));
            log.info("{} {} files with content type {}", operation, fileSource.getFiles().size(), fileSource.getMediaType());
            log.info("Target content type: {}", targetContentType);
            StreamPseudonymizer pseudonymizer = pseudonymizerFactory.newStreamPseudonymizer(request.getPseudoConfig().getRules(), fileSource.getMediaType());
            Flowable<String> res = processStream(operation, fileSource.getInputStream(), targetContentType, pseudonymizer);

            TargetCompression targetCompression = request.getCompression();
            if (targetCompression != null) {
                log.info("Applying target compression: " + MoreMediaTypes.APPLICATION_ZIP_TYPE);
                String contentFilename = (operation + "-" + System.currentTimeMillis() + "." + targetContentType.getExtension()).toLowerCase();
                res = serialize(res, targetContentType);
                return new ProcessFileResult(MediaType.APPLICATION_OCTET_STREAM_TYPE, Zips.zip(res, contentFilename, zipOpts()
                  .password(targetCompression.getPassword())
                  .encryptionMethod(CompressionEncryptionMethod.AES)
                  .build()
                ));
            }

            return new ProcessFileResult(targetContentType, res);
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

    private static Flowable<String> serialize(Flowable<String> recordStream, MediaType targetContentType) {
        if (targetContentType.equals(MediaType.APPLICATION_JSON)) {
            AtomicBoolean first = new AtomicBoolean(true);
            return recordStream
              .map(record -> {
                  if (first.getAndSet(false)) {
                      return "[%s".formatted(record);
                  }
                  return ",%s".formatted(record);
              })
              .concatWith(Single.just("]"));
        }

        return recordStream;
    }

    private Flowable<String> processStream(PseudoOperation operation, InputStream is, MediaType targetContentType, StreamPseudonymizer pseudonymizer) {
        return operation == PseudoOperation.PSEUDONYMIZE
          ? pseudonymizer.pseudonymize(is, RecordMapSerializerFactory.newFromMediaType(targetContentType))
          : pseudonymizer.depseudonymize(is, RecordMapSerializerFactory.newFromMediaType(targetContentType));
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
    public static class PseudoRequest {

        /**
         * The pseudonymization config to apply
         */
        private PseudoConfig pseudoConfig;

        /**
         * Specify this if you want to stream the result to a specific location such as a GCS bucket. Note that the pseudo
         * service needs to have access to the bucket. Leave this unspecified in order to just stream the result back to
         * the client.
         */
        @Schema(implementation = String.class)
        private URI targetUri;

        /** The content type of the resulting file. */
        @Schema(implementation = String.class, allowableValues = {
          MediaType.APPLICATION_JSON, MoreMediaTypes.TEXT_CSV})
        private MediaType targetContentType;

        /**
         * Specify this if you want to compress and password protect the payload. The archive will be encrypted with AES256.
         */
        private TargetCompression compression;
    }

    @Data
    public static class ProcessFileResult {
        @Schema(implementation = String.class)
        private final MediaType targetContentType;

        private final Flowable flowable;
    }

    @Data
    public static class TargetCompression {

        /** The password on the resulting archive */
        @NotBlank
        @Schema(implementation = String.class)
        @Min(9)
        private char[] password;
    }
}
