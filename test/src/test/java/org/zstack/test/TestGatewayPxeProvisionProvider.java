package org.zstack.test;

import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.server.*;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Deterministic OSS test-only provider for GATEWAY_PXE.
 * Captures the ProvisionRequest so tests can assert PhysicalServer-first fields.
 * Not imported by any premium BM2 code.
 */
public class TestGatewayPxeProvisionProvider implements ProvisionProvider {

    private final AtomicReference<ProvisionRequest> lastRequest = new AtomicReference<>();

    @Override
    public ProvisionNetworkType getType() {
        return ProvisionNetworkType.GATEWAY_PXE;
    }

    @Override
    public void prepareNetwork(PhysicalServerProvisionNetworkInventory network,
                               String poolUuid,
                               Completion completion) {
        completion.success();
    }

    @Override
    public void destroyNetwork(PhysicalServerProvisionNetworkInventory network,
                               String poolUuid,
                               Completion completion) {
        completion.success();
    }

    @Override
    public void startProvisioning(ProvisionRequest request,
                                  ReturnValueCompletion<ProvisionResult> completion) {
        lastRequest.set(request);
        ProvisionResult result = new ProvisionResult()
                .setServerUuid(request.getServerUuid())
                .setNetworkUuid(request.getNetworkUuid())
                .setProviderType(getType().toString())
                .setProviderResourceUuid(request.getServerUuid());
        completion.success(result);
    }

    public ProvisionRequest getLastRequest() {
        return lastRequest.get();
    }

    public void reset() {
        lastRequest.set(null);
    }
}
