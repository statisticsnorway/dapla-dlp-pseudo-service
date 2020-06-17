package no.ssb.dlp.pseudo.service;

import com.google.common.io.Files;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Part;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.multipart.StreamingFileUpload;
import io.reactivex.Flowable;
import io.reactivex.Single;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.ssb.dlp.pseudo.service.util.Json;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RequiredArgsConstructor
@Controller
@Slf4j
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
        return processFile(request, data, headers, PseudoOperation.PSEUDONYMIZE);
    }

    @Post("/depseudonymize/file")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces({MediaType.APPLICATION_JSON, MoreMediaTypes.TEXT_CSV})
    public HttpResponse<Flowable> depseudonymizeFile(
      @Part("request") String requestString,
      StreamingFileUpload data,
      HttpHeaders headers) {
        PseudoRequest request = Json.toObject(PseudoRequest.class, requestString);
        return processFile(request, data, headers, PseudoOperation.DEPSEUDONYMIZE);
    }

    private HttpResponse<Flowable> processFile(PseudoRequest request, StreamingFileUpload data, HttpHeaders headers, PseudoOperation operation) {
        MediaType sourceContentType = data.getContentType().orElseThrow(() -> new PseudoException("Unable to determine data content type"));
        MediaType targetContentType = targetContentType(headers.accept());
        log.info("{} file with content-type {} ({} MB)", operation, sourceContentType, (data.getSize() / 1000000));
        log.info("Target content type: {}", targetContentType);
        File tempFile = null;
        try {
            tempFile = receiveFile(data).blockingGet();
            InputStream is = Files.asByteSource(tempFile).openBufferedStream();
            StreamPseudonymizer pseudonymizer = pseudonymizerFactory.newStreamPseudonymizer(request.getPseudoConfig().getRules(), sourceContentType);
            Flowable res = processStream(operation, is, targetContentType, pseudonymizer);
            return HttpResponse.ok(res).contentType(targetContentType);
        }
        catch (IOException e) {
            return HttpResponse.serverError(Flowable.error(e));
        }
        finally {
            if (tempFile != null) {
                tempFile.delete();
            };
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
        File tempFile = File.createTempFile(data.getFilename(), "temp");
        log.debug("Receive file - stored temporarily at " + tempFile.getAbsolutePath());
        return Single.fromPublisher(data.transferTo(tempFile))
          .map(success -> {
              if (success) {
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
}
