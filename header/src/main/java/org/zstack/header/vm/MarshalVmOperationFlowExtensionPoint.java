package org.zstack.header.vm;

import org.zstack.header.core.workflow.FlowChain;

/**
 * Created by frank on 7/2/2015.
 */
public interface MarshalVmOperationFlowExtensionPoint {
    FlowChain marshalVmOperationFlows(FlowChain chain, VmInstanceSpec spec);
}
