package org.zstack.header.identity;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.APIMessage;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface AdditionalLoginExtensionPoint {
    ErrorCode authenticate(APIMessage msg, String resourceUuid, String resourceType);

    default List<Map<String, String>> additionLoginRequirement(String resourceUuid, String resourceType) {
        return Collections.emptyList();
    }
}
