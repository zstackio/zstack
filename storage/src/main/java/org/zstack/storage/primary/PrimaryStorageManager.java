package org.zstack.storage.primary;

import org.zstack.header.storage.primary.PrimaryStorageFactory;
import org.zstack.header.storage.primary.PrimaryStorageType;

public interface PrimaryStorageManager {
	PrimaryStorageFactory getPrimaryStorageFactory(PrimaryStorageType type);
	
    void sendCapacityReportMessage(long total, long avail, String primaryStorageUuid);
}
