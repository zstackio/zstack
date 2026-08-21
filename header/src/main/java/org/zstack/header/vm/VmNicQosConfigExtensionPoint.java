package org.zstack.header.vm;

import org.zstack.header.errorcode.ErrorCode;

public interface VmNicQosConfigExtensionPoint {
    ErrorCode validateVmNicQos(String l3NetworkUuid);
}
