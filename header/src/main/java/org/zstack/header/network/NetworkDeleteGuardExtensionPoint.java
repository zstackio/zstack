package org.zstack.header.network;

import org.zstack.header.errorcode.ErrorCode;

public interface NetworkDeleteGuardExtensionPoint {
    ErrorCode checkL3Network(String l3NetworkUuid);

    default ErrorCode checkL3NetworkWithLock(String l3NetworkUuid) {
        return checkL3Network(l3NetworkUuid);
    }
}
