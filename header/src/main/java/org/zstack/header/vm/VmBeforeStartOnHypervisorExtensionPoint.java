package org.zstack.header.vm;

import org.zstack.header.core.Completion;

/**
 */
public interface VmBeforeStartOnHypervisorExtensionPoint {
    default void beforeStartVmOnHypervisor(VmInstanceSpec spec) {}

    default void beforeStartVmOnHypervisor(VmInstanceSpec spec, Completion completion) {
        beforeStartVmOnHypervisor(spec);
        completion.success();
    }
}
