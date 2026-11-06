package org.zstack.tag;

import org.zstack.header.message.APIMessage;

public interface SystemTagApiMessageExtensionPoint {
    String getResourceUuid(APIMessage msg);

    String getSyncSignature(String resourceUuid);

    void beforeHandle(APIMessage msg);
}
