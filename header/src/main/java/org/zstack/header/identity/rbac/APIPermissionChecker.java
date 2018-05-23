package org.zstack.header.identity.rbac;

import org.zstack.header.message.APIMessage;

public interface APIPermissionChecker {
    Boolean check(APIMessage msg);
}
