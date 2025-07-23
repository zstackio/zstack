package org.zstack.header.network.service;

import org.zstack.header.network.l3.SdnControllerL3;

public interface GetSdnControllerExtensionPoint {
    SdnControllerDhcp getSdnControllerDhcp(String l3Uuid);
    SdnControllerL3 getSdnControllerL3(String l2Uuid);
}
