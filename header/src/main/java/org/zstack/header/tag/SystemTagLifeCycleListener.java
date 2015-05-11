package org.zstack.header.tag;

/**
 */
public interface SystemTagLifeCycleListener {
    void tagCreated(SystemTagInventory tag);

    void tagDeleted(SystemTagInventory tag);
}
