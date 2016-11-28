package org.zstack.header.managementnode;


public interface ManagementNodeChangeListener {
    void nodeJoin(String nodeId);

    void nodeLeft(String nodeId);

    void iAmDead(String nodeId);

    void iJoin(String nodeId);
}
