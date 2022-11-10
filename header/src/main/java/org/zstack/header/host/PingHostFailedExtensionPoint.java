package org.zstack.header.host;

import org.zstack.header.errorcode.ErrorCode;

public interface PingHostFailedExtensionPoint {
    void afterPingHostFailed(String hostUuid, ErrorCode errorCode);
}
