package org.zstack.physicalserver;

public interface PhysicalServerMessage {
    /**
     * @return the target PhysicalServer UUID
     */
    String getServerUuid();
}
