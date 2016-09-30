package org.zstack.test.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostErrors.Opaque;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.kvm.KVMPingAgentExtensionPoint;

/**
 * Created by xing5 on 2016/8/6.
 */
public class KVMPingAgentExtensionForTest implements KVMPingAgentExtensionPoint {
    public volatile boolean success = true;

    @Autowired
    private ErrorFacade errf;

    @Override
    public void kvmPingAgent(KVMHostInventory host, Completion completion) {
        if (success) {
            completion.success();
        } else {
            ErrorCode err = errf.stringToOperationError("on purpose");
            err.putToOpaque(Opaque.NO_RECONNECT_AFTER_PING_FAILURE.toString(), true);
            completion.fail(err);
        }
    }
}
