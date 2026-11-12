package org.zstack.header.vm;

import org.zstack.header.core.workflow.Flow;

import java.util.List;

public interface VmOperationAdditionalFlowExtensionPoint {
    List<Flow> getBeforeFormalWorkFlows(VmInstanceSpec spec);

    List<Flow> getAdditionalVmOperationFlows(VmInstanceSpec spec);
    /** Runs after the operation's formal flows and before its success callback. */
    default List<Flow> getAfterFormalWorkFlows(VmInstanceSpec spec) {
        return java.util.Collections.emptyList();
    }
}
