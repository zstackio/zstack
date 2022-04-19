package org.zstack.kvm;

import org.zstack.header.core.workflow.Flow;

public interface PrepareKVMHostExtensionPoint {
    Flow createPrepareKvmHostFlow(KVMHostConnectedContext context);
}
