package org.zstack.header.identity;

public interface AdminAuthorization {
    boolean isGlobalAdmin(SessionInventory session);
}
