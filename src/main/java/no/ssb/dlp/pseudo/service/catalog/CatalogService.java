package no.ssb.dlp.pseudo.service.catalog;

import io.reactivex.Single;
import lombok.Builder;
import lombok.Data;
import no.ssb.dapla.dataset.api.DatasetState;
import no.ssb.dapla.dataset.api.Valuation;
import no.ssb.dapla.dataset.uri.DatasetUri;

import java.util.ArrayList;
import java.util.List;

public interface CatalogService {

    String SERVICE_ID = "dapla-catalog";
    String CONFIG_PREFIX = "micronaut.http.services." + SERVICE_ID;

    Single<GetDatasetResponse> getDataset(GetDatasetRequest request);

    @Data
    @Builder
    class GetDatasetRequest {
        private String path;
        private String timestamp;
    }

    @Data
    class GetDatasetResponse {
        private Dataset dataset;
    }

    @Data
    class Dataset {
        private DatasetId id;
        private Valuation valuation;
        private DatasetState state;
        private String parentUri;
        private PseudoConfig pseudoConfig;

        public DatasetUri datasetUri() {
            return DatasetUri.of(parentUri, id.getPath(), id.getTimestamp());
        }
    }

    @Data
    class DatasetId {
        private String path;
        private String timestamp;
    }

    @Data
    class PseudoConfig {
        private List<PseudoVar> vars = new ArrayList<>();
    }

    @Data
    class PseudoVar {
        private String var;
        private String pseudoFunc;
    }

}
