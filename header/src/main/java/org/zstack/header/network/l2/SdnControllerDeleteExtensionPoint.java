package org.zstack.header.network.l2;

import org.zstack.header.core.Completion;

public interface SdnControllerDeleteExtensionPoint {
    void deleteNetworkServiceOfSdnController(String sdnControllerUuid, Completion completion);
}
