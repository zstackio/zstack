package org.zstack.header.network.l2;

import org.zstack.header.errorcode.ErrorCode;

/** Provider hook for confirmed remote-first L2 deletion. */
public interface L2DeleteConfirmExtensionPoint {
    boolean supports(L2NetworkInventory inventory);
    ErrorCode begin(L2NetworkInventory inventory);
    ErrorCode check(L2NetworkInventory inventory);
    ErrorCode delete(L2NetworkInventory inventory);
    ErrorCode cancel(L2NetworkInventory inventory);
    void deleteLocalMetadata(L2NetworkInventory inventory);
}
