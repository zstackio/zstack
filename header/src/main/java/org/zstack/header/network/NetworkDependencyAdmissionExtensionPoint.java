package org.zstack.header.network;

import org.zstack.header.errorcode.ErrorCode;

public interface NetworkDependencyAdmissionExtensionPoint {
    ErrorCode admit(NetworkDependencyAdmissionRequest request);
}
