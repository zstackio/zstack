package org.zstack.network.service.flat;

import java.util.Map;
import java.util.List;

public interface L3NetworkGetIpStatisticExtensionPoint {
    String getApplianceVmInstanceType();
    List<String> getParentUuid(String uuid, String vipUuid);
}
