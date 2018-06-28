package org.zstack.header.identity;

import org.zstack.header.message.APIMessage;

public interface AdditionalLoginExtensionPoint {
    boolean authenticate(APIMessage msg, String resourceUuid, String resourceType);
}
