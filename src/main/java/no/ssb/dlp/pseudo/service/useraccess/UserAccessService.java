package no.ssb.dlp.pseudo.service.useraccess;

import io.reactivex.Single;
import lombok.Builder;
import lombok.Data;
import no.ssb.dapla.dataset.api.DatasetState;
import no.ssb.dapla.dataset.api.Valuation;
import no.ssb.dapla.dataset.uri.DatasetUri;

import java.util.ArrayList;
import java.util.List;

public interface UserAccessService {

    String SERVICE_ID = "dapla-user-access";
    String CONFIG_PREFIX = "micronaut.http.services." + SERVICE_ID;

    Single<Boolean> hasAccess(String userId, DatasetPrivilege privilege, String path, Valuation valuation, DatasetState state);

    default Single<Boolean> hasAccess(AccessCheckRequest req) {
        return hasAccess(req.getUserId(), req.getPrivilege(), req.getPath(), req.getValuation(), req.getState());
    }

    @Data
    @Builder
    class AccessCheckRequest {
        private String userId;
        private DatasetPrivilege privilege;
        private String path;
        private Valuation valuation;
        private DatasetState state;
    }

    enum DatasetPrivilege {
        CREATE, READ, UPDATE, DELETE, DEPSEUDO;
    }

}
