package org.zstack.header.network.service;

public interface NetworkServiceAttachExtensionPoint {
    boolean skipAttachNetworkService(APIAttachNetworkServiceToL3NetworkMsg msg);
}
