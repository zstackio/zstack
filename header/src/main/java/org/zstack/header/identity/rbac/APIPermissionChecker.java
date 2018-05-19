package org.zstack.header.identity.rbac;

import org.zstack.header.message.APIMessage;

public interface APIPermissionChecker {
    boolean check(APIMessage msg);
}
