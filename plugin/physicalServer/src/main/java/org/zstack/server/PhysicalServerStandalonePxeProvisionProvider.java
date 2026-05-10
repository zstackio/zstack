package org.zstack.server;

import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.server.*;

import static org.zstack.core.Platform.operr;

public class PhysicalServerStandalonePxeProvisionProvider implements ProvisionProvider {
    @Override
    public ProvisionNetworkType getType() {
        return ProvisionNetworkType.STANDALONE_PXE;
    }

    @Override
    public void prepareNetwork(PhysicalServerProvisionNetworkInventory network, String poolUuid, Completion completion) {
        completion.success();
    }

    @Override
    public void destroyNetwork(PhysicalServerProvisionNetworkInventory network, String poolUuid, Completion completion) {
        completion.success();
    }

    @Override
    public void startProvisioning(ProvisionRequest request, ReturnValueCompletion<ProvisionResult> completion) {
        completion.fail(operrf("STANDALONE_PXE ProvisionProvider is reserved and not implemented yet"));
    }

    private ErrorCode operrf(String fmt, Object... args) {
        return operr(SysErrors.OPERATION_ERROR.toString(), fmt, args);
    }
}
