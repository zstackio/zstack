package org.zstack.kvm;

import org.zstack.header.core.workflow.Flow;

public interface KVMHostPreConnectExtensionPoint {
    Flow createKvmHostPreConnectingFlow(KVMHostConnectedContext context);
}
