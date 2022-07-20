package no.ssb.dlp.pseudo.service.datasetmeta;

import com.google.protobuf.util.JsonFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.ssb.dapla.dataset.api.DatasetMeta;
import no.ssb.dapla.dataset.uri.DatasetUri;
import no.ssb.dapla.storage.client.backend.BinaryBackend;
import no.ssb.dlp.pseudo.core.PseudoFuncRule;
import no.ssb.dlp.pseudo.service.pseudo.PseudoConfig;

import javax.inject.Singleton;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Singleton
@Slf4j
public class DatasetMetaService {

    private final BinaryBackend storageBackend;

    public Optional<DatasetMeta> readDatasetMeta(DatasetUri datasetUri) {
        String datasetMetaPath = datasetUri.toString() + "/.dataset-meta.json";
        log.info("Reading dataset metadata from {}", datasetMetaPath);

        try {
            final SeekableByteChannel channel;
            try {
                channel = storageBackend.read(datasetMetaPath);
            }
            catch (NullPointerException e) {
                log.info("No dataset metadata found");
                return Optional.empty();
            }

            int bufferSize = Math.min(1024, (int) channel.size());
            ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            while (channel.read(buffer) > 0) {
                out.write(buffer.array(), 0, buffer.position());
                buffer.clear();
            }
            String datasetMetaJson = new String(out.toByteArray(), StandardCharsets.UTF_8);

            DatasetMeta.Builder datasetMetaBuilder = DatasetMeta.newBuilder();
            JsonFormat.parser().ignoringUnknownFields().merge(datasetMetaJson, datasetMetaBuilder);
            return Optional.of(datasetMetaBuilder.build());
        }
        catch (Exception e) {
            throw new DatasetMetaReadException("Unable to read dataset meta for " + datasetUri.toString(), e);
        }
    }

    public PseudoConfig readDatasetPseudoConfig(DatasetUri datasetUri) {
        log.debug("Read pseudo rules from " + datasetUri);
        DatasetMeta datasetMeta = readDatasetMeta(datasetUri).orElse(null);
        return pseudoConfigOf(datasetMeta);
    }

    public PseudoConfig pseudoConfigOf(DatasetMeta datasetMeta) {
        PseudoConfig pseudoConfig = new PseudoConfig();
        if (datasetMeta == null) {
            return pseudoConfig;
        }

        AtomicInteger ruleNo = new AtomicInteger();
        pseudoConfig.setRules(datasetMeta.getPseudoConfig().getVarsList().stream()
            .map(i -> new PseudoFuncRule("pseudo-rule-" + ruleNo.getAndIncrement(), i.getVar(), i.getPseudoFunc()))
            .toList()
        );

        return pseudoConfig;
    }

    public static class DatasetMetaReadException extends RuntimeException {
        public DatasetMetaReadException(String message, Throwable cause) {
            super(message, cause);
        }
    }


}
