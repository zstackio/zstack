package org.zstack.header.identity.extension;

import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.APIMessage;

public interface AuthorizationBackend {
    boolean takeOverAuthorization(SessionInventory session);

    APIMessage authorize(APIMessage msg);
}
