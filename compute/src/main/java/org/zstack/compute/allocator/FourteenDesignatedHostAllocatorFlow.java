package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.core.retry.Retry;
import org.zstack.core.retry.RetryCondition;
import org.zstack.header.allocator.AbstractHostAllocatorFlow;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.rest.RESTFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.io.IOException;
import java.util.ArrayList;

public class FourteenDesignatedHostAllocatorFlow extends AbstractHostAllocatorFlow {
    @Autowired
    private RESTFacade restf;
    private static final CLogger logger = Utils.getLogger(FourteenDesignatedHostAllocatorFlow.class);

    private void allocate(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json;charset=utf-8");
        HttpEntity<String> req = new HttpEntity<>("Host", headers);
        ResponseEntity<String> rsp = new Retry<ResponseEntity<String>>() {
            @Override
            @RetryCondition(onExceptions = {IOException.class, HttpStatusCodeException.class})
            protected ResponseEntity<String> call() {
                return restf.getRESTTemplate().exchange(url, HttpMethod.POST, req, String.class);
            }
        }.run();

        if (rsp.getStatusCode().is2xxSuccessful()) {
            String s = rsp.getBody();
            if (candidates == null) {
                candidates = new ArrayList<>();
            }
            if (s != null) {
                HostVO o = Q.New(HostVO.class).eq(HostVO_.uuid, s).find();
                if (o == null) {
                    fail(Platform.operr("host candidates not found"));
                }

                candidates.add(o);
            }
            next(candidates);
        } else {
            fail(Platform.operr("failed to connect server: %s", url));
        }
    }

    @Override
    public void allocate() {
        allocate(HostAllocatorGlobalConfig.GET_HOST_VM_UUID_URL.value());
    }
}
