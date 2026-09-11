package org.zstack.header.network.l3;

public interface L3NetworkTypeConversionExtensionPoint {
    /**
     * Called inside the conversion transaction with the L3 row locked.
     * Recheck IP/NIC dependencies under the owner's allocation lock; do not mutate them.
     */
    boolean canPreserveIpReservations(ConvertL3NetworkTypeMsg msg, L3NetworkInventory network);
}
