package org.zstack.header.vm;

import org.zstack.header.core.Completion;

/**
 */
public interface VmBeforeCreateOnHypervisorExtensionPoint {
    void beforeCreateVmOnHypervisor(VmInstanceSpec spec);

    default void beforeCreateVmOnHypervisor(VmInstanceSpec spec, Completion completion) {
        beforeCreateVmOnHypervisor(spec);
        completion.success();
    }
}
