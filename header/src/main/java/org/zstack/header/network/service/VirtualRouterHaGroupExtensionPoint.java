package org.zstack.header.network.service;

import org.zstack.header.core.Completion;
import org.zstack.header.message.MessageReply;

import java.util.List;

public interface VirtualRouterHaGroupExtensionPoint {
    String getPublicIp(String vrUuid, String l3Uuid);

    /* virtual router vm type */
    String getL3NetworkServiceProviderTypeOfHaRouter(String l3NetworkUuid);

    void VirtualRouterVmHaAttachL3Network(String vrUuid, String l3NetworkUuid, Completion completion);
    void VirtualRouterVmHaDetachL3Network(String vrUuid, String l3NetworkUuid, boolean isRollback, Completion completion);
    Boolean isVirtualRouterInSameHaPair(List<String> vrUuids);

    void afterReceiveTracerReply(String resourceUuid, MessageReply reply);

    void syncVirtualRouterHaConfig(String vrUuid, Completion completion);

    void prepareVirtualRouterHaConfig(String vrUuid, Completion completion);
}
