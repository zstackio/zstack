package org.zstack.network.service.eip;

import org.zstack.header.core.Completion;

import java.util.List;

/**
 */
public interface EipBackend {
    void applyEip(EipStruct struct, Completion completion);

    void revokeEip(EipStruct struct, Completion completion);

    String getNetworkServiceProviderType();

    void applyEip(String vrUuid, EipStruct struct, Completion completion);

    void revokeEip(String vrUuid, EipStruct struct, Completion completion);

    void attachEipToVirtualRouter(List<String> eipUuids, String vrUuid);

    void detachEipFromVirtualRouter(List<String> eipUuids, String vrUuid);

    List<String> getEipUuidsOnVirtualRouter(String vrUuid);
}
