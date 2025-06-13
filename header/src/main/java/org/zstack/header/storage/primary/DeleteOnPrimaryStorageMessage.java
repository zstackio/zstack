package org.zstack.header.storage.primary;

public interface DeleteOnPrimaryStorageMessage extends PrimaryStorageMessage {
    boolean isGcOnFailure();
}
