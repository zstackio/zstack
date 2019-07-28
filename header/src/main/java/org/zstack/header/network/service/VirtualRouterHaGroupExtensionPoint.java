package org.zstack.header.network.service;

import org.zstack.header.core.Completion;
import org.zstack.header.message.MessageReply;

import java.util.List;
import java.util.Map;

public interface VirtualRouterHaGroupExtensionPoint {

    String getPublicIp(String vrUuid, String l3Uuid);

    void VirtualRouterVmHaAttachL3Network(String vrUuid, String l3NetworkUuid, Completion completion);
    void VirtualRouterVmHaDetachL3Network(String vrUuid, String l3NetworkUuid, boolean isRollback, Completion completion);
    Boolean isVirtualRouterInSameHaPair(List<String> vrUuids);

    void afterReceiveTracerReply(String resourceUuid, MessageReply reply);

    void syncVirtualRouterHaConfigToBackend(String vrUuid, Completion completion);

    String getPeerUuid(String vrUuid);

    void submitTaskToHaRouter(VirtualRouterHaCallbackInterface callback, Map<String, Object> data, Completion completion);

    void attachNetworkServiceToHaRouter(String type, List<String> uuids, String vrUuid);
    void attachNetworkServiceToHaRouter(String type, List<String> uuids, String vrUuid, boolean override);
    void detachNetworkServiceFromHaRouter(String type, List<String> uuids, String vrUuid);
    List<String> getHaVrUuidsFromNetworkService(String type, String Uuid);
    List<String> getNetworkServicesFromHaVrUuid(String type, String vrUuid);

    void cleanupHaNetworkService(String vrUuid, Completion completion);
}
