package org.zstack.header.identity;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.APIMessage;

public interface AdditionalLoginExtensionPoint {
    ErrorCode authenticate(APIMessage msg, String resourceUuid, String resourceType);
}
