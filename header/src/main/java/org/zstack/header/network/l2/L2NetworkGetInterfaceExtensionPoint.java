package org.zstack.header.network.l2;


import java.util.List;

public interface L2NetworkGetInterfaceExtensionPoint {
    List<String> getPhysicalInterfaceNames(String l2NetworkUuid, String hostUuid);
    L2NetworkType getType();
}
