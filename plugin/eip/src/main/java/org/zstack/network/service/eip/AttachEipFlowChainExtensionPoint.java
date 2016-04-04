package org.zstack.network.service.eip;

import org.zstack.header.core.workflow.FlowChainProcessor;

/**
 * Created by xing5 on 2016/4/4.
 */
public interface AttachEipFlowChainExtensionPoint {
    FlowChainProcessor createAttachEipFlowChainProcessor(EipStruct struct, String providerType);
}
