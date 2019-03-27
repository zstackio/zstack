package org.zstack.identity.rbac;

import org.zstack.header.identity.SessionInventory;

public interface PolicyCreationExtensionPoint {
    boolean checkPermission(SessionInventory session);
}
